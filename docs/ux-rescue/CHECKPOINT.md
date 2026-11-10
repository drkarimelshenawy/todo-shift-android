# Preserved rescue checkpoint

Baseline 6a0acb7; source stages 34e75bc and 71ec61c. Candidate 4.2.0 (31), com.todoshift.app. All app source is committed on feat/neo-luxury-4.1-ultimate. Final local verification: 132 regular tests and 2 separate native probe tests pass; lint zero errors/45 warnings; assembly/signature/identity/alignment/source checks pass. Reports reconcile 130 original requirements plus 12 corrections without claiming full user acceptance.

Final delivery documentation/workflow commit is the next stage. Fetch and fast-forward push only the authorized feature ref; dispatch android.yml at exact final SHA. Record CI result and independently check downloaded artifact. Stop on divergence/auth failure. Never merge/release/uninstall/clear storage. Installed-app certificate remains unverified; local and CI certificates differ.

Evidence is preserved outside git at /workspace/output/todo-shift-4.2.0. Historical reports remain historical. No source restart or repeated modernization required. See REMAINING.md for unfinished device/manual acceptance.
