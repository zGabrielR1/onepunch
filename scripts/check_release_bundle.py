"""Hosted-only smoke test of the extracted Linux distribution with disposable data."""
import json
from pathlib import Path
import subprocess
import tarfile
import tempfile
import time
import urllib.error
import urllib.request
from release_config import read_release_config


def main():
    version = read_release_config()['version']
    archive = Path(f'build/release/onepunch-v{version}-linux-amd64.tar.gz')
    with tempfile.TemporaryDirectory(prefix='onepunch-bundle-') as temporary:
        root = Path(temporary)
        with tarfile.open(archive, 'r:gz') as package:
            package.extractall(root, filter='data')
        bundle = root / 'onepunch-linux-amd64'
        with (root / 'api.log').open('w') as log:
            process = subprocess.Popen(
                [str(bundle / 'server/onepunch-api'), '-addr', '127.0.0.1:18098',
                 '-data', str(root / 'state.json')], cwd=bundle / 'server', stdout=log, stderr=log)
            try:
                origin = 'http://127.0.0.1:18098'
                for _ in range(60):
                    if process.poll() is not None:
                        raise RuntimeError('Packaged API exited: ' + (root / 'api.log').read_text())
                    try:
                        with urllib.request.urlopen(origin + '/api/health', timeout=1) as response:
                            json.load(response)
                        break
                    except (urllib.error.URLError, TimeoutError):
                        time.sleep(0.25)
                else:
                    raise RuntimeError('Packaged API did not become ready')
                with urllib.request.urlopen(origin, timeout=3) as response:
                    page = response.read().decode()
                if '<html' not in page or '/assets/' not in page:
                    raise RuntimeError('Packaged web shell was not served')
                with urllib.request.urlopen(origin + '/api/exercises', timeout=3) as response:
                    actual = json.load(response)
                if actual != json.loads((bundle / 'shared/catalog.json').read_text()):
                    raise RuntimeError('Packaged exercise catalog differs')
                print('Extracted Linux API/web bundle smoke test passed')
            finally:
                process.terminate()
                try:
                    process.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait()


if __name__ == '__main__':
    main()
