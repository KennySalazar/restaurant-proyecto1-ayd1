"""Deploy the approved main commit; credentials come only from Actions secrets."""
import json
import os
from pathlib import Path
import re
import subprocess
import time
import urllib.error
import urllib.request

PROJECTS = ('restaurante-admin', 'restaurante-pos')
BACKEND = 'https://restaurant-proyecto1-ayd1.onrender.com'


class DeploymentError(Exception):
    pass


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def request(url, token=None, data=None):
    headers = {'Accept': 'application/json', 'User-Agent': 'restaurant-ci'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    if data is not None:
        headers['Content-Type'] = 'application/json'
    req = urllib.request.Request(url, headers=headers,
                                 data=json.dumps(data).encode() if data is not None else None)
    try:
        with urllib.request.build_opener(NoRedirect).open(req, timeout=30) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        raise DeploymentError(f'API respondió HTTP {error.code}; revisar el panel del proveedor.') from None
    except (urllib.error.URLError, TimeoutError, ValueError):
        raise DeploymentError('No se pudo obtener una respuesta JSON válida del proveedor.') from None


def wait_for(probe, seconds):
    deadline = time.monotonic() + seconds
    while True:
        result = probe()
        if result:
            return result
        if time.monotonic() >= deadline:
            raise DeploymentError('Tiempo de espera agotado; comprobar el panel antes de reintentar.')
        time.sleep(10)


def deploy(env, report):
    required = ('GH_TOKEN', 'GITHUB_SHA', 'GITHUB_REPOSITORY', 'RENDER_API_KEY',
                'RENDER_SERVICE_ID', 'CLOUDFLARE_API_TOKEN', 'CLOUDFLARE_ACCOUNT_ID', 'WRANGLER_BIN')
    for name in required:
        if not env.get(name):
            raise DeploymentError(f'Falta configurar {name}.')
    if env.get('GITHUB_EVENT_NAME') != 'push' or env.get('GITHUB_REF') != 'refs/heads/main':
        raise DeploymentError('Solo un push a main puede desplegar.')
    sha = env['GITHUB_SHA']
    if not re.fullmatch(r'[0-9a-f]{40}', sha):
        raise DeploymentError('SHA inválido.')
    report['commit'] = sha
    for name in ('RENDER_SERVICE_ID', 'CLOUDFLARE_ACCOUNT_ID'):
        if not re.fullmatch(r'[a-zA-Z0-9-]+', env[name]):
            raise DeploymentError(f'Formato inválido de {name}.')
    for project in PROJECTS:
        if not Path('deploy-builds', project, 'index.html').is_file():
            raise DeploymentError(f'Falta el artefacto aprobado de {project}.')

    def current_main():
        ref = request(f"https://api.github.com/repos/{env['GITHUB_REPOSITORY']}/git/ref/heads/main", env['GH_TOKEN'])
        if ref['object']['sha'] != sha:
            raise DeploymentError('main avanzó; esta ejecución antigua no debe publicar.')

    render = f"https://api.render.com/v1/services/{env['RENDER_SERVICE_ID']}"
    cf = f"https://api.cloudflare.com/client/v4/accounts/{env['CLOUDFLARE_ACCOUNT_ID']}/pages/projects"

    def pages(project):
        response = request(f'{cf}/{project}-ayd1', env['CLOUDFLARE_API_TOKEN'])
        if response.get('success') is not True:
            raise DeploymentError('Cloudflare rechazó la consulta del proyecto.')
        return response['result']

    # Read-only preflight for BOTH sites before starting any deployment.
    for project in PROJECTS:
        if pages(project).get('production_branch') != 'main':
            raise DeploymentError(f'{project}: configurar la rama de producción main en Pages.')
    service = request(render, env['RENDER_API_KEY'])
    if service.get('branch') != 'main' or service.get('autoDeploy') != 'no':
        raise DeploymentError('Render debe usar main y Auto-Deploy Off.')
    current_main()
    created = request(render + '/deploys', env['RENDER_API_KEY'],
                      {'commitId': sha, 'clearCache': 'do_not_clear'})
    deploy_id = created['id']
    report['render'] = {'id': deploy_id, 'status': 'requested'}

    def render_live():
        result = request(f'{render}/deploys/{deploy_id}', env['RENDER_API_KEY'])
        status = result['status']
        report['render']['status'] = status
        if status in ('build_failed', 'update_failed', 'pre_deploy_failed', 'canceled', 'deactivated'):
            raise DeploymentError('Render no completó el despliegue; Pages no se publicará.')
        if status == 'live':
            if result.get('commit', {}).get('id') != sha:
                raise DeploymentError('El commit publicado en Render no coincide con el aprobado.')
            return True
        return False

    wait_for(render_live, 1500)
    if request(BACKEND + '/api/v1/actuator/health/readiness').get('status') != 'UP':
        raise DeploymentError('Readiness del backend no está UP.')
    # Do not pass Render or GitHub credentials to Wrangler.
    child_env = {key: value for key, value in env.items()
                 if key in ('PATH', 'HOME', 'CI', 'CLOUDFLARE_API_TOKEN',
                            'CLOUDFLARE_ACCOUNT_ID', 'WRANGLER_SEND_METRICS')}
    for project in PROJECTS:
        current_main()
        report[project] = {'status': 'requested'}
        result = subprocess.run([env['WRANGLER_BIN'], 'pages', 'deploy',
                                 f'deploy-builds/{project}', '--project-name', project + '-ayd1',
                                 '--branch', 'main', '--commit-hash', sha, '--commit-dirty=false'],
                                env=child_env, capture_output=True, timeout=300)
        if result.returncode:
            raise DeploymentError(f'Falló Wrangler para {project}; revisar Deployments en Pages.')

        def pages_live():
            deployment = pages(project).get('canonical_deployment') or {}
            metadata = deployment.get('deployment_trigger', {}).get('metadata', {})
            if (deployment.get('environment') == 'production'
                    and metadata.get('commit_hash') == sha
                    and deployment.get('latest_stage', {}).get('status') == 'success'):
                report[project] = {'status': 'success', 'id': deployment['id']}
                return True
            return False

        wait_for(pages_live, 120)


def main():
    report = {'status': 'failed'}
    try:
        deploy(dict(os.environ), report)
        report['status'] = 'success'
    except DeploymentError as error:
        report['error'] = str(error)
    except Exception:
        # Provider bodies, CLI output and environment values must never enter logs.
        report['error'] = 'Respuesta inesperada o herramienta interrumpida; revisar los paneles antes de reintentar.'
    finally:
        Path('deploy-reports').mkdir(exist_ok=True)
        content = json.dumps(report, indent=2) + '\n'
        Path('deploy-reports/production.json').write_text(content)
        print(content)
        if os.environ.get('GITHUB_STEP_SUMMARY'):
            with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as summary:
                summary.write('### Despliegue de producción\n```json\n' + content + '```\n')
    return 0 if report['status'] == 'success' else 1


if __name__ == '__main__':
    raise SystemExit(main())
