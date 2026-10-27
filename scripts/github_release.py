"""Small repository-specific GitHub API client. No credentials are printed or saved.

Uses the existing Git credential helper; never prints tokens or stores them in files.
Builds are dispatched to GitHub-hosted runners, never performed by this script.
"""
import argparse
import io
import json
import os
import subprocess
import sys
import urllib.error
import urllib.request
import zipfile

sys.dont_write_bytecode = True
from release_config import read_release_config

BASE = "https://api.github.com/repos/zGabrielR1/onepunch"


class SafeRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        redirected = super().redirect_request(req, fp, code, msg, headers, newurl)
        if redirected and not newurl.startswith("https://api.github.com/"):
            redirected.remove_header("Authorization")
        return redirected


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=["status", "runs", "jobs", "logs", "dispatch", "verify"])
    parser.add_argument("--run", type=int)
    parser.add_argument("--tag", default="v" + read_release_config()["version"])
    args = parser.parse_args()
    env = dict(os.environ, GIT_TERMINAL_PROMPT="0")
    credential = subprocess.run(
        ["git", "credential", "fill"], input="protocol=https\nhost=github.com\n\n",
        text=True, capture_output=True, env=env, check=True,
    )
    fields = dict(line.split("=", 1) for line in credential.stdout.splitlines() if "=" in line)
    token = fields.get("password")
    if not token:
        raise RuntimeError("No GitHub credential is available from Git's credential helper.")
    opener = urllib.request.build_opener(SafeRedirect())

    def request(path, data=None, binary=False):
        req = urllib.request.Request(
            BASE + path, data=None if data is None else json.dumps(data).encode(),
            headers={"Authorization": "Bearer " + token, "Accept": "application/octet-stream" if binary else "application/vnd.github+json",
                     "User-Agent": "onepunch-release", "Content-Type": "application/json"},
        )
        try:
            with opener.open(req, timeout=60) as response:
                raw = response.read()
        except urllib.error.HTTPError as error:
            # Report only the public error message, never request headers/credentials.
            try:
                message = json.loads(error.read()).get("message", "Request failed")
            except (ValueError, AttributeError):
                message = "Request failed"
            raise RuntimeError(f"GitHub HTTP {error.code}: {message}") from None
        return raw if binary else json.loads(raw) if raw else None

    if args.action == "status":
        repo = request("")
        releases = request("/releases?per_page=5")
        print(json.dumps({"repository": repo["full_name"], "private": repo["private"],
                          "default_branch": repo["default_branch"], "permissions": repo.get("permissions"),
                          "releases": [{"tag": r["tag_name"], "draft": r["draft"], "url": r["html_url"]} for r in releases],
                          "actions": request("/actions/permissions")}, indent=2))
    elif args.action == "verify":
        release = request("/releases/tags/" + args.tag)
        assets = release["assets"]
        manifest_asset = next(asset for asset in assets if asset["name"] == "verification.json")
        manifest = json.loads(request(f"/releases/assets/{manifest_asset['id']}", binary=True))
        if "commit" not in manifest:
            raise RuntimeError("The published verification manifest could not be read.")
        print(json.dumps({"url": release["html_url"], "draft": release["draft"],
                          "prerelease": release["prerelease"],
                          "assets": [{"name": a["name"], "size": a["size"], "digest": a.get("digest")} for a in assets],
                          "verification": manifest}, indent=2))
    elif args.action == "runs":
        runs = request("/actions/runs?per_page=5")["workflow_runs"]
        print(json.dumps([{k: r[k] for k in ["id", "name", "head_sha", "status", "conclusion", "html_url"]} for r in runs], indent=2))
    elif args.action == "dispatch":
        request("/actions/workflows/release.yml/dispatches", {"ref": "main"})
        print("Dispatched hosted release workflow on main.")
    elif args.action == "jobs":
        if not args.run:
            parser.error("--run is required")
        jobs = request(f"/actions/runs/{args.run}/jobs")["jobs"]
        print(json.dumps([{k: j[k] for k in ["id", "name", "status", "conclusion", "steps"]} for j in jobs], indent=2))
    elif args.action == "logs":
        if not args.run:
            parser.error("--run is required")
        raw = request(f"/actions/runs/{args.run}/logs", binary=True)
        with zipfile.ZipFile(io.BytesIO(raw)) as archive:
            for name in archive.namelist():
                if name.endswith(".txt") and "/" not in name:
                    lines = archive.read(name).decode("utf8", errors="replace").splitlines()
                    print("LOG:", name)
                    print("\n".join(lines[-160:]))


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
