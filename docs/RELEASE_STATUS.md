# 1.0 readiness and defect ledger

Baseline reviewed 2026-10-27. Display name remains provisional Onepunch;
application/storage identity remains stable. Distribution target is the existing
private GitHub APK and Linux amd64 bundle channel. No account hosting or store
publication is included.

## Client support inventory

Source-reviewed; current working-tree native changes still require hosted checks.

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
| R1 | Blocker | Existing APKs use ephemeral debug signing; no signing secrets configured at review | Owner signing choice pending; release config, signed build and fingerprint gate implemented; actual key/build unverified |
| R2 | Blocker | Device/picker/interruption and signed upgrade QA not recorded | Native QA script plus exact-artifact JSON report/promotion checks prepared; real device evidence pending |
| R3 | Blocker | Current working tree contains native changes beyond published 0.3.0 | Source reviewed; compile/JVM checks on hosted candidate pending |
| R4 | Blocker | Old pipeline could publish a debug APK under a stable version | Debug workflow now rejects stable settings; separate signed candidate and hash-checked promotion path implemented; regression/source checks required |
| R5 | Blocker | Archive startup and final artifact QA not established | Clean-extraction API/web smoke check added to signed workflow; hosted execution pending |

Historical v0.3.0 hosted run 40558518832 passed 54 JVM tests, four Go race tests
and 116 browser checks. Build-only run 40606230105 from d80e67e subsequently
completed successfully; those results do not cover this working tree. Do not carry historical
pass counts forward as evidence for a new candidate.

Native working changes already present at implementation start include manual
rest controls, routine-draft exercise replacement, exercise comparisons and stricter
workspace-envelope parsing. They remain separate from release-tooling verification.

This release increment also adds raw original-record export on unreadable local
workspace/draft screens. This preserves malformed strings for assisted recovery
without replacing local state; its two JVM regression cases await hosted execution.

No blocker is closed merely because a document or check has been added. Link its
successful hosted result/device evidence and exact source/artifact before closure.
