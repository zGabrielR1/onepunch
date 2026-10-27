"""Hosted CI only: summarize actual test results and bind artifacts to their source."""
import json
import os
from pathlib import Path
import re
import xml.etree.ElementTree as ET
from release_config import read_release_config

reports = list(Path("android/app/build/test-results/testDebugUnitTest").glob("TEST-*.xml"))
if not reports:
    raise SystemExit("No Android JVM reports found; refusing to produce verification metadata.")
totals = {key: 0 for key in ("tests", "failures", "errors", "skipped")}
for report in reports:
    suite = ET.parse(report).getroot()
    for key in totals:
        totals[key] += int(suite.attrib.get(key, 0))
if totals["tests"] == 0 or totals["failures"] or totals["errors"]:
    raise SystemExit("Android JVM verification did not pass.")
browser_log = Path("build-browser-checks.txt").read_text()
match = re.search(r"(\d+) browser checks passed", browser_log)
if not match:
    raise SystemExit("Browser workflow result is missing.")
go_events = [json.loads(line) for line in Path("build-go-tests.json").read_text().splitlines() if line.strip()]
if any(event.get("Action") == "fail" for event in go_events):
    raise SystemExit("Go verification failed.")
go_tests = sum(event.get("Action") == "pass" and "Test" in event for event in go_events)
if not go_tests:
    raise SystemExit("No successful Go test results found.")
manifest = {
    "version": read_release_config()["version"],
    "commit": os.environ["GITHUB_SHA"],
    "workflow": f"https://github.com/{os.environ['GITHUB_REPOSITORY']}/actions/runs/{os.environ['GITHUB_RUN_ID']}",
    "android_jvm": totals,
    "go_race_tests": go_tests,
    "browser_workflow_checks": int(match[1]),
    "web_domain_tests": "passed before production build",
    "android_device_ui": "not tested; requires separate device QA",
    "apk_signing": "ephemeral debug certificate; not a production signing identity",
    "build_location": "GitHub-hosted Ubuntu runner; no local native compilation",
}
Path("build/release/verification.json").write_text(json.dumps(manifest, indent=2) + "\n")
