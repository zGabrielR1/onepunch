# 1.0 readiness and defect ledger

Baseline reviewed 2026-10-27. Display name remains provisional Onepunch;
application/storage identity remains stable. Distribution target is the existing
private GitHub APK and Linux amd64 bundle channel. No account hosting or store
publication is included.

## Current source — 2026-11-08

RC.2 contains 100 exercises and 17 templates plus assistance metrics, immutable
finished-history and web recovery fixes. The product tree was verified with changes based on `cfdc351`; signed RC.2
publication is now explicitly authorized but has not yet run.

Automated verification is complete for this development source:

- Android debug compilation and 77 JVM tests passed: 14 XML suites, zero
  failures/errors/skips. APK SHA-256:
  `44ddfeb14d870e55b6b452d2257fbe74dbbcd017664d688fc42a8906dcc59f94`.
- Host web run `build/verification-sZwVOAeV` passed 16 domain/catalog tests,
  13 release-tooling tests, production build, Go race tests, 181 workflow browser
  checks and 52 catalog/layout browser checks: 233 browser assertions total.
- All 53 recorded source hashes matched at the automation review. Publication
  tooling was added afterward; product source remains unchanged.
  Reviewed the 320px enlarged-text workout and picker screenshots; controls and
  no-results feedback are visible without horizontal clipping in those states.
- Jobs enforced the 3 GiB/no-swap cap; the host reported web peak 1.6G. Native
  results came from the preceding host run `build/verification-XDpBtAva`.

Durable evidence summary: [RC.2 automated QA](qa/v1_0_0_rc_2_automated.json).
This does not establish signed-release artifact identity or Android device UI QA.
Fresh signed-candidate, exact-artifact physical-device QA and independent signing
key backup remain required before final promotion.

## Client support inventory

The support table reflects current source and automated development checks.
Real-device behavior remains pending exact-candidate QA; the historical signed
RC.1 run does not verify RC.2.

| Capability | Native Android | Web companion |
| --- | --- | --- |
| Offline routine/free workout logging | Yes, bundled assets | Yes; first online load needed for offline shell |
| Incomplete active inputs/completed sets/rest deadline recovery | Yes | Yes |
| Editable templates and routine editor | Yes | Yes |
| Recoverable routine draft, duplication/archive/restore | Yes | No complete lifecycle/draft parity |
| Active exercise order/replacement, manual rest start/shorten | Yes | Not at native parity |
| Finish review/history/recorded-load metrics | Yes | Yes; narrower review flow |
| History search/date filters and workout replay | Yes | Not at native parity |
| JSON backup/import and pre-restore safety copy | Yes | No UI |
| Optional routines/finished-history sync | Yes | Yes |
| Active workout/draft sync | Intentionally local only | Active intentionally local only |
| Background rest alarms | Not promised | Not promised |

## Release blockers

| ID | Severity | Finding / reproduction | Resolution and evidence |
| --- | --- | --- | --- |
| R1 | Blocker | Existing APKs use ephemeral debug signing | Permanent owner-authorized key configured; signed run 40607494288 verified certificate and non-debuggable APK. Local recovery files preserved outside Git; independent owner key backup still needs confirmation |
| R2 | Blocker | Device/picker/interruption and signed upgrade QA not recorded | Native QA script plus exact-artifact JSON report/promotion checks prepared; real device evidence pending |
| R3 | Closed | Native changes beyond published 0.3.0 required verification | Signed run 40607494288 for 3a7c705 passed release compilation and 70 JVM tests, zero failures/errors/skips; debug run 40607216559 also passed |
| R4 | Closed | Old pipeline could publish a debug APK under a stable version | Debug workflow rejects stable settings; signed candidate/promotion gates implemented. 13 release-tooling tests passed on signed run 40607494288 |
| R7 | Automated regression passed; device QA pending | Native sync used timestamp replacement for immutable history; finish could replace a colliding ID | Reject conflicting content at every timestamp and finish collisions; shared backup comparator, specific UI error, four new JVM regressions passed in the 77-test host run; conflict UI still requires device QA |
| R8 | Closed by domain/browser regressions | Empty saved web value silently seeded a workspace; malformed nested records could escape recovery or poison sync | Preserve unreadable empty storage, validate nested shape before loading/merging; Domain regressions and all 16 added browser assertions passed in host run verification-sZwVOAeV |
| R6 | Automated regression passed; device QA pending | Assistance was counted as lifted load and ranked as maximum load | Both clients preserve raw assistance/history but exclude it from lifted volume and load records; units are explicit. Web regression passes; two Android JVM regressions passed in the 77-test host run; all 45 added browser assertions passed in host run verification-sZwVOAeV. No schema migration |
| R5 | Automated gate passed; manual QA pending | Archive startup and final artifact QA not established | Signed run 40607494288 passed extracted Linux bundle smoke test, four Go race tests, 116 browser checks and web checks/build. Final-version/device QA remains R2 |

Historical v0.3.0 hosted run 40558518832 passed 54 JVM tests, four Go race tests
and 116 browser checks. Build-only run 40606230105 from d80e67e subsequently
completed successfully; those results do not cover this working tree. Do not carry historical
pass counts forward as evidence for a new candidate.

Native working changes already present at implementation start include manual
rest controls, routine-draft exercise replacement, exercise comparisons and stricter
workspace-envelope parsing. They remain separate from release-tooling verification.

This release increment also adds raw original-record export on unreadable local
workspace/draft screens. This preserves malformed strings for assisted recovery
without replacing local state; its two JVM regression cases passed in the signed run.

Signed 1.0.0-rc.1 (code 4) files are in `build/signed-40607494288/`; all downloaded
SHA256SUMS entries passed. Exact-artifact pending device report:
`docs/qa/v1_0_0_rc_1.json`. No device tests or stable publication were performed.

No blocker is closed merely because a document or check has been added. Link its
successful hosted result/device evidence and exact source/artifact before closure.

Signed prerelease shipment is prepared through candidate.yml's opt-in publication
job. Eight new publication regressions bring release-tooling checks to21 passing.
Fresh hosted signed build/publication awaits owner-host GitHub CLI execution; this
session lacks GitHub authentication/network access. Stable gates remain open.
