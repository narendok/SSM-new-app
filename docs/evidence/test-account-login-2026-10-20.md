# Android TEST sign-in — 20 October 2026

Pulled remote b57665b (test personas and inventory search). Corrected password sign-in to fail closed: removed fabricated demo JWT fallback, clear old session before account changes, resolve existing TEST membership from the authoritative backend, derive workspace presentation from that response, and discard old fabricated sessions. Authentication events run in MainViewModel.viewModelScope. The debug chooser contains 26 existing synthetic accounts: admin, twelve leads, twelve employees and client; no users/passwords created or reset. Google Workspace entry remains available.

Validation: testDebugUnitTest and assembleDebug passed, 35 tests, zero failures. One existing FactCheck icon deprecation warning remains. No Android device/emulator acceptance performed. Debug APK: app/build/outputs/apk/debug/app-debug.apk.

Independent live backend acceptance used existing admin, EHW lead, QA lead, FW lead and EHW employee identities. All signed in and resolved membership successfully. Synthetic project HLPL-OBD0374783-01: employee task update became visible to EHW lead; delegated QA approval changed the EHW evidence status to approved; EHW signing QA approval was rejected; unrelated FW could not read the evidence. No production records or credentials changed.

Web main de71805 includes the deployed shared TEST migration; normalized deployment SQL equals original migration 0008. Web TypeScript and Drizzle checks pass. This verifies the shared TEST core, not complete Android domain integration: existing local prototype inventory/project screens are not shared backend records. Full device workflow, private-file and lifecycle acceptance remain pending.

Follow-up: corrected the TEST organization Unicode separator comparison; restored authenticated workspace presentation from saved server membership on reopen; local role selector routes authenticated accounts to real sign-in; repeated sign-in taps are serialized. Rebuilt successfully with all 35 tests passing. Device testing remains user-owned via Android Studio wireless debugging.
