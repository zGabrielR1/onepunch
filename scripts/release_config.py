"""Validate one release identity before the hosted build or publication."""
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parent.parent


def read_release_config():
    values = {}
    for line in (ROOT / "shared/release.properties").read_text(encoding="utf8").splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        key, value = line.split("=", 1)
        if key in values:
            raise ValueError("Duplicate release setting: " + key)
        values[key] = value
    version = values["version"]
    if not re.fullmatch(r"\d+\.\d+\.\d+(?:-[a-zA-Z0-9.-]+)?", version):
        raise ValueError("Invalid release version")
    if not 1 <= int(values["androidVersionCode"]) <= 2_000_000_000:
        raise ValueError("Invalid Android version code")
    if values["prerelease"] not in ("true", "false"):
        raise ValueError("Invalid prerelease setting")
    package = json.loads((ROOT / "web/package.json").read_text())
    lock = json.loads((ROOT / "web/package-lock.json").read_text())
    if any(value != version for value in (package["version"], lock["version"], lock["packages"][""]["version"])):
        raise ValueError("Web package versions disagree with shared/release.properties")
    if not (ROOT / f"docs/releases/v{version}.md").is_file():
        raise ValueError("Release notes are missing")
    return values


if __name__ == "__main__":
    config = read_release_config()
    print("RELEASE_VERSION=" + config["version"])
    print("RELEASE_TAG=v" + config["version"])
    print("RELEASE_TITLE=OnePunch " + config["version"])
    print("RELEASE_NOTES=docs/releases/v" + config["version"] + ".md")
    print("RELEASE_PRERELEASE=" + config["prerelease"])
