"""Smoke test local/CI. Requires Docker and the locally built API image; no cloud credentials."""
import json, os, secrets, subprocess, time, urllib.request, urllib.error
from pathlib import Path
suffix = secrets.token_hex(6)
if not __debug__:
    raise RuntimeError("Run without Python optimization: assertions are required")
repo = Path(__file__).resolve().parents[2]
migrations = sorted((repo / "restaurante-api/src/main/resources/db/migration").glob("V*__*.sql"),
                    key=lambda path: int(path.name.split("__")[0][1:]))
expected_versions = [path.name.split("__")[0][1:] for path in migrations]
assert expected_versions, "No versioned migrations found"
network, db, api = ['restaurante-check-' + part + '-' + suffix for part in ['net', 'db', 'api']]
created = []
def docker(*args, env=None):
    p = subprocess.run(['docker', *args], env=env, text=True, capture_output=True)
    if p.returncode:
        raise RuntimeError('Docker operation failed: ' + args[0])
    return p.stdout.strip()
def request(path, method='GET', data=None, headers=None):
    req=urllib.request.Request(base+path, method=method,
        data=None if data is None else json.dumps(data).encode(), headers=headers or {})
    try:
        with urllib.request.urlopen(req, timeout=5) as r: return r.status, r.read(), r.headers
    except urllib.error.HTTPError as e: return e.code, e.read(), e.headers
try:
    docker('network', 'create', network)
    env=os.environ.copy()
    env.update(POSTGRES_PASSWORD=secrets.token_urlsafe(30))
    docker('run', '-d', '--name', db, '--network', network, '--tmpfs', '/var/lib/postgresql',
           '-e', 'POSTGRES_PASSWORD', '-e', 'POSTGRES_USER=smoke', '-e', 'POSTGRES_DB=smoke', 'postgres:18', env=env)
    created.append(db)
    for _ in range(30):
        p=subprocess.run(['docker','exec',db,'pg_isready','-U','smoke','-d','smoke'],capture_output=True)
        if p.returncode == 0: break
        time.sleep(1)
    else: raise RuntimeError('Postgres did not become ready')
    env.update(DATABASE_URL=f'jdbc:postgresql://{db}:5432/smoke',DATABASE_USERNAME='smoke',
        DATABASE_PASSWORD=env['POSTGRES_PASSWORD'],SECRET_KEY_JWT=secrets.token_urlsafe(40),
        INITIAL_ADMIN_EMAIL='admin@example.invalid',INITIAL_ADMIN_PASSWORD='Aa1'+secrets.token_hex(15),
        BREVO_API_KEY='local-test-not-a-real-key',MAIL_FROM='sender@example.invalid',PORT='18080',
        CORS_ALLOWED_ORIGINS='https://admin.example.invalid,https://pos.example.invalid')
    args=['run','-d','--name',api,'--network',network,'--memory','512m','--memory-swap','512m',
          '--cpus','0.5','-p','127.0.0.1::18080']
    for key in ['DATABASE_URL','DATABASE_USERNAME','DATABASE_PASSWORD','SECRET_KEY_JWT',
                'INITIAL_ADMIN_EMAIL','INITIAL_ADMIN_PASSWORD','BREVO_API_KEY','MAIL_FROM','PORT','CORS_ALLOWED_ORIGINS']:
        args.extend(['-e',key])
    args.append('restaurante-api:local-prod-check')
    docker(*args,env=env)
    created.append(api)
    port=docker('port',api,'18080/tcp').rsplit(':',1)[1]
    base='http://127.0.0.1:'+port+'/api/v1'
    def wait_ready():
        for _ in range(90):
            try:
                status,body,_=request('/actuator/health/readiness')
                if status==200:
                    assert json.loads(body)=={'status':'UP'}
                    return
            except (OSError, TimeoutError): pass
            if docker('inspect','--format','{{.State.Running}}',api)!='true':
                raise RuntimeError('API exited before readiness; inspect isolated test logs')
            time.sleep(2)
        raise RuntimeError('API readiness timed out')
    started=time.monotonic()
    wait_ready()
    print('PASS: prod startup, custom PORT, readiness without details; seconds:',round(time.monotonic()-started),flush=True)
    def history():
        return docker('exec',db,'psql','-U','smoke','-d','smoke','-Atc',
            'SELECT version || chr(58) || success FROM flyway_schema_history ORDER BY installed_rank')
    first=history()
    assert first.splitlines()==[version+':true' for version in expected_versions], 'Migration history mismatch'
    print('PASS: all', len(expected_versions), 'migrations applied to fresh PostgreSQL 18',flush=True)
    for path in ['/auth/me','/admin/ping','/actuator/health','/actuator/env']:
        assert request(path)[0]==401, path
    assert request('/v3/api-docs')[0]==200
    status,body,_=request('/auth/login','POST',{'email':env['INITIAL_ADMIN_EMAIL'],
        'password':env['INITIAL_ADMIN_PASSWORD']},{'Content-Type':'application/json'})
    assert status==200, 'login failed'
    token=json.loads(body)['accessToken']
    assert token
    assert request('/auth/me',headers={'Authorization':'Bearer '+token})[0]==200
    assert request('/admin/ping',headers={'Authorization':'Bearer '+token})[0]==200
    print('PASS: bootstrap, login, JWT, protected routes, OpenAPI',flush=True)
    for origin,expected in [('https://admin.example.invalid',200),('https://pos.example.invalid',200),('https://attacker.example.invalid',403)]:
        status,_,headers=request('/auth/me','OPTIONS',headers={'Origin':origin,
            'Access-Control-Request-Method':'GET','Access-Control-Request-Headers':'authorization,content-type'})
        assert status==expected, 'CORS status mismatch'
        assert headers.get('Access-Control-Allow-Origin') == (origin if expected==200 else None)
    print('PASS: real SecurityFilterChain CORS preflight and untrusted origin rejection',flush=True)
    print('Memory sample:',docker('stats','--no-stream','--format','{{.MemUsage}}',api),flush=True)
    docker('restart',api)
    port=docker('port',api,'18080/tcp').rsplit(':',1)[1]
    base='http://127.0.0.1:'+port+'/api/v1'
    wait_ready()
    assert history()==first
    assert request('/auth/me',headers={'Authorization':'Bearer '+token})[0]==200
    print('PASS: restart preserves migration history and authenticated session',flush=True)
    print('Runtime UID:',docker('exec',api,'id','-u'),flush=True)
    print('PASS: all local production smoke checks; no Neon or Brevo requests',flush=True)
finally:
    for name in reversed(created):
        subprocess.run(['docker','stop',name],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
    print('Test containers stopped; no existing containers or volumes modified.',flush=True)
