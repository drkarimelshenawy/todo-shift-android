#!/usr/bin/env python3
"""Static integration guardrails. Not a substitute for Android build/instrumentation."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
home = (root / "app/src/main/java/com/todoshift/app/v11/ui/V11Home.kt").read_text()
host = (root / "app/src/main/java/com/todoshift/app/integration/EmbeddedCashBookScreen.kt").read_text()
settings = (root / "settings.gradle.kts").read_text()
app = (root / "app/build.gradle.kts").read_text()
gateway = (root / "app/src/main/java/com/todoshift/app/integration/SalaryCashPostingGateway.kt").read_text()

assert 'include(":app", ":cashbook-core", ":cashbook-feature")' in settings
assert 'implementation(project(":cashbook-feature"))' in app
assert 'lifecycle-viewmodel-compose' in app
assert 'CashBookRoute(vm = vm' in host
assert 'com.todoshift.app.integration.EmbeddedCashBookScreen()' in home
assert 'neoLiteral("كاش بوك", "Cash Book")' in home
assert 'neoLabel("كاش بوك", "Cash Book")' in home

# Require the Cash Book destination to use the same unlock policy as finance.
cashbook_start = home.index('                                    1 ->\n                                        if (\n                                            securitySettings.protectAccounts')
cashbook_end = home.index('                                    2 ->', cashbook_start)
cashbook = home[cashbook_start:cashbook_end]
for token in ('securitySettings.biometricEnabled', '!accountsUnlocked', 'V11AccountsUnlockScreen(', 'EmbeddedCashBookScreen()'):
    assert token in cashbook, f'Cash Book security guard missing: {token}'

assert 'salary-receipt:$paymentId' in gateway
assert 'CashIncomePort' in gateway
print('PASS: Cash Book v44 integration structural checks')
