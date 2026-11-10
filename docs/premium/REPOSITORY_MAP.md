# Repository map — 10 November 2026

## Working reference

`premium/stable-candidate` is the current Premium reference under device testing, not a production-stability certification. Its production application remains byte-identical to commit `2bc80943c90215cecea0db9c037f59ee9915f3cb`. Maintenance commits change workflows, build metadata, documentation and the lifecycle synchronization of one test helper. The exact original application AND tests are preserved by tag `premium-stable-candidate`.

The warm-finance test originally sent its intent immediately after Activity recreation and intermittently timed out before checking PIN. It now waits for a ready Calendar host and uses ActivityScenario's resumed Activity. The Finance/PIN/private-content assertions remain; no test is disabled. This does not change the APK's application code or establish delivery-during-boot coverage.

Do not transplant app changes from `premium-rescue-archived` into this branch without a separate reviewed task. Reported phone crashes, missing shift colors and branding remain unresolved device findings; successful CI is not a claim that those issues disappeared.

## Four kept branches

| Branch | Purpose |
|---|---|
| `main` | Existing default branch; not modified by cleanup |
| `Luxury-v4` | Historical Neo Luxury reference; not modified |
| `premium/stable-candidate` | Preserved Premium application source plus current build infrastructure |
| `fix/neo-luxury-4.1-data-integrity` | Open PR #1 into Luxury-v4; not deleted or merged |

## Archive tags

| Tag | Original commit |
|---|---|
| `premium-stable-candidate` | `2bc80943c90215cecea0db9c037f59ee9915f3cb` |
| `premium-rescue-archived` | `ef4281b867472d753bf2163d82062c4562cd08e8` |
| `neo-luxury-4.1-archive` | `e178b134658afad04d645b044f24d236594cbe33` |
| `archive/Premium-V-byWork-2026-11-10` | `7ad1f0658df968b213179a129e7facec05f11961` |
| `archive/rc9-luxury-upgrade-2026-11-10` | `520534c0e341ba24b425200c2edbeb55f879c007` |
| `archive/v0.9-development-2026-11-10` | `e500c383dd3ca25fc7e5fa3dacfc92de105638ff` |
| `archive/v0.5-development-2026-11-10` | `2ff68c2b4e4aec0c195e4c1864cc9db06b7187ec` |

Tags are lightweight source archives, not GitHub releases or endorsements of those APKs. A tag is usable for restoration only after its remote SHA has been verified. Cleanup refuses to replace a conflicting tag. The maintenance run summary is authoritative about which operations actually completed.

## Build entry points

- `premium-qa.yml`: manual or reusable QA on this branch, checked source SHA, ephemeral test signing, unit/integration/Robolectric tests, lint and APK build. The ephemeral APK stays inside the runner; only evidence is uploaded.
- `android.yml`: manual, workflow-change-triggered or reusable baseline test build with the existing CI signing secrets and required historical certificate. APK package/version are verified against `tools/PREMIUM_CANDIDATE.json`, not hardcoded to version 31. No release is published.
- `premium-maintenance.yml`: one-time cleanup, triggered by changes to that workflow or manually. Archives all seven references, runs both build paths, then deletes only the six explicitly listed branch tips using SHA leases. It checks kept branches, open PRs and active candidate-branch workflow runs, and fails closed on unexpected changes. Source must match the preserved baseline under `app/`.

The obsolete rescue branch workflows remain available in its archive tag. No old branch names are needed by the active workflow configuration. References in historical reports are retained as historical evidence.

GitHub Actions artifacts expire according to retention settings; tags preserve source, not APK files or signing keys. Phone acceptance and upgrade/restore verification remain separate from repository cleanup and CI.
- The source-parity guard compares all of `app/` except test-source directories. Test synchronization edits do not ship in the application; production resources, manifest, build definition and Kotlin sources remain protected by the comparison.
