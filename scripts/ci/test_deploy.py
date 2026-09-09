import unittest
from unittest.mock import Mock, patch
import deploy


class DeploymentTests(unittest.TestCase):
    def setUp(self):
        self.env = dict(GH_TOKEN='test-gh', GITHUB_SHA='a' * 40,
                        GITHUB_REPOSITORY='example/repo', RENDER_API_KEY='test-render',
                        RENDER_SERVICE_ID='srv-test', CLOUDFLARE_API_TOKEN='test-cf',
                        CLOUDFLARE_ACCOUNT_ID='testaccount', WRANGLER_BIN='/test/wrangler',
                        GITHUB_EVENT_NAME='push', GITHUB_REF='refs/heads/main')
        self.status = 'live'
        self.branch = 'main'
        self.head = self.env['GITHUB_SHA']
        self.render_sha = self.head
        self.posts = []
        self.report = {}

    def api(self, url, token=None, data=None):
        if data is not None:
            self.posts.append(data)
            return {'id': 'dep-test'}
        if 'api.github.com' in url:
            return {'object': {'sha': self.head}}
        if '/pages/projects/' in url:
            return {'success': True, 'result': {
                'production_branch': self.branch,
                'canonical_deployment': {'id': 'cf-test', 'environment': 'production',
                    'deployment_trigger': {'metadata': {'commit_hash': self.head}},
                    'latest_stage': {'status': 'success'}}}}
        if '/deploys/' in url:
            return {'status': self.status, 'commit': {'id': self.render_sha}}
        if 'onrender.com' in url:
            return {'status': 'UP'}
        return {'branch': 'main', 'autoDeploy': 'no'}

    def run_deploy(self):
        with patch.object(deploy, 'request', side_effect=self.api), \
             patch.object(deploy.Path, 'is_file', return_value=True), \
             patch.object(deploy.subprocess, 'run', return_value=Mock(returncode=0)) as cli:
            deploy.deploy(self.env, self.report)
            return cli

    def test_success_uses_approved_commit_and_isolates_credentials(self):
        cli = self.run_deploy()
        self.assertEqual(self.posts, [{'commitId': self.head, 'clearCache': 'do_not_clear'}])
        self.assertEqual(cli.call_count, 2)
        for call in cli.call_args_list:
            self.assertIn(self.head, call.args[0])
            self.assertNotIn('RENDER_API_KEY', call.kwargs['env'])
            self.assertNotIn('GH_TOKEN', call.kwargs['env'])
        self.assertEqual(self.report['restaurante-pos']['status'], 'success')

    def test_pr_and_develop_cannot_deploy(self):
        for key, value in [('GITHUB_EVENT_NAME', 'pull_request'), ('GITHUB_REF', 'refs/heads/develop')]:
            with self.subTest(key=key), patch.dict(self.env, {key: value}):
                with self.assertRaises(deploy.DeploymentError):
                    self.run_deploy()
        self.assertEqual(self.posts, [])

    def test_missing_secret_fails_before_any_mutation(self):
        del self.env['CLOUDFLARE_API_TOKEN']
        with self.assertRaisesRegex(deploy.DeploymentError, 'CLOUDFLARE_API_TOKEN'):
            self.run_deploy()
        self.assertEqual(self.posts, [])

    def test_wrong_pages_branch_fails_before_render(self):
        self.branch = 'production'
        with self.assertRaises(deploy.DeploymentError):
            self.run_deploy()
        self.assertEqual(self.posts, [])

    def test_stale_main_fails_before_render(self):
        self.head = 'b' * 40
        with self.assertRaises(deploy.DeploymentError):
            self.run_deploy()
        self.assertEqual(self.posts, [])

    def test_render_failure_or_wrong_commit_never_calls_pages_cli(self):
        for status, sha in [('build_failed', self.head), ('live', 'b' * 40)]:
            self.status, self.render_sha = status, sha
            with self.subTest(status=status), patch.object(deploy, 'request', side_effect=self.api), \
                 patch.object(deploy.Path, 'is_file', return_value=True), \
                 patch.object(deploy.subprocess, 'run') as cli:
                with self.assertRaises(deploy.DeploymentError):
                    deploy.deploy(self.env, {})
                cli.assert_not_called()

    def test_first_pages_failure_stops_second_site(self):
        with patch.object(deploy, 'request', side_effect=self.api), \
             patch.object(deploy.Path, 'is_file', return_value=True), \
             patch.object(deploy.subprocess, 'run', return_value=Mock(returncode=1)) as cli:
            with self.assertRaises(deploy.DeploymentError):
                deploy.deploy(self.env, {})
            self.assertEqual(cli.call_count, 1)

    def test_polling_has_deadline(self):
        with patch.object(deploy.time, 'monotonic', side_effect=[0, 121]):
            with self.assertRaises(deploy.DeploymentError):
                deploy.wait_for(lambda: False, 120)

    def test_http_error_does_not_expose_response_or_credentials(self):
        error = deploy.urllib.error.HTTPError('https://example.test/private', 401, 'secret-body', {}, None)
        with patch.object(deploy.urllib.request, 'build_opener') as opener:
            opener.return_value.open.side_effect = error
            with self.assertRaises(deploy.DeploymentError) as caught:
                deploy.request('https://example.test', 'secret-token')
            self.assertNotIn('secret', str(caught.exception))
            self.assertIn('401', str(caught.exception))


if __name__ == '__main__':
    unittest.main()
