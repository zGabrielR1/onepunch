# Onepunch — Linux API and web companion

This bundle contains a personal, single-user sync API and the compiled web app.
It runs on Linux amd64. It has no account authentication and binds loopback by
default. Keep it local; public hosting is outside this release's scope.

From the extracted `onepunch-linux-amd64` directory:

```sh
cd server
./onepunch-api
```

Open http://127.0.0.1:8097. The default relative paths load the catalog from
`../shared/catalog.json`, the web app from `../web/dist`, and save sync records
in `data/state.json` beneath the current directory. Keep this data directory
outside any directory you replace when upgrading. An explicit persistent path:

```sh
./onepunch-api -data /your/persistent/directory/state.json
```

Only one API process may own a state file. Stop the process before copying that
file for backup or replacing the binary. Preserve the old bundle and data copy
until the new bundle has been checked. Never run QA against personal data.

The browser stores its own workspace in localStorage. After the first successful
online load the production app shell supports offline reload; clearing browser
storage removes those local records. Sync exchanges routines and finished
workouts only. Active workouts remain on the originating client. Native Android
JSON backup also includes active inputs and routine drafts; a server snapshot
does not substitute for that backup.

Before use, verify the downloaded release files with `sha256sum -c SHA256SUMS.txt`
from the directory containing all listed release files. Check the release notes
for the source commit, signing identity, device QA and upgrade limitations.
