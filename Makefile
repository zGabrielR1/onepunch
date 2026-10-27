GO ?= go
NPM ?= npm
.PHONY: deps web api run dev test check-light android

check-light:
	node --max-old-space-size=128 scripts/check-light.mjs

deps:
	$(NPM) ci --prefix web

web:
	$(NPM) run build --prefix web

api:
	mkdir -p build
	cd server && $(GO) build -buildvcs=false -o ../build/onepunch-api .

run: web api
	cd server && ../build/onepunch-api

dev:
	$(NPM) run dev --prefix web

test:
	$(NPM) test --prefix web
	cd server && $(GO) test -race ./...

android:
	cd android && ./gradlew :app:assembleDebug :app:testDebugUnitTest
	mkdir -p build
	cp android/app/build/outputs/apk/debug/app-debug.apk build/onepunch-debug.apk
