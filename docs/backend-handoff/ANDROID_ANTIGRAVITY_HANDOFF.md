# Android Companion Handoff for Antigravity

Read this file when working on the Android companion from the SSM1 Antigravity workspace. The portal's architecture and current integration status are in [SSM One Web Portal Master Plan, §25](../SSM_ONE_WEB_PORTAL_MASTER_PLAN.md#25-android-companion-handoff-contract-and-release-gates). The department-owned document and lifecycle audit design is in [HLPL-AI4T-QA-DOCCTRL-00](HLPL-AI4T-QA-DOCCTRL-00.md).

## Android project location

The Android Studio project currently resides at:

`C:\Users\Test\AndroidStudioProjects\SSM-new-app`

The Android project is a separate Git repository at `https://github.com/narendok/SSM-new-app.git` on `main`. It is outside the SSM1 web folder, so Android commits do not automatically land in the portal repository or Lovable sync. Work directly in this Android repository; do not create a second Android source copy under SSM1.

## Product identity

- Web portal display name: **SSM New** (`narendok/ssm-new`).
- Android launcher and Gradle project name: **SSM-new-app** (`narendok/SSM-new-app`).
- Keep Android `applicationId` and namespace stable when changing display names.

## Corrected backend configuration

- The SSM1 portal's configured Supabase authority is project `rzskhndweihuczbdhoxk` at `https://rzskhndweihuczbdhoxk.supabase.co`.
- Android Gradle now defaults to this endpoint, rejects a different `SUPABASE_URL`, and fails configuration if the publishable/anon key is empty.
- The Android project's ignored `local.properties` contains the same URL and the portal publishable key as `SUPABASE_ANON_KEY`. Keep the key out of tracked files, screenshots, logs, and handoff documents.
- Do not use the historical Android target `yyrvduosyyaluifqvwkr` as a current environment. No separate Dev/UAT backend is approved.

## Files to consult in the Android project

- `app/build.gradle.kts` — canonical URL guard and allowed client configuration.
- `docs/backend-handoff/ANDROID_BACKEND_HANDOFF.md` — mobile security and operation boundaries.
- `docs/backend-handoff/MOBILE_BACKEND_CONTRACT.md` and `mobile_backend_contract.json` — proposed read/RPC contract; confirm every object against the portal's actual schema before implementation.
- `docs/planning/SSM_ONE_ANDROID_BUILD_PLAN.md` — Android delivery plan.
- `app/src/main/java/com/example/myapplication/data/repository/SafeViewsRepository.kt` and `StockMutationRepository.kt` — currently fail-closed pending verified schema/RPC contracts.

## Remaining release gates

1. Reconcile Android models, views, RPC signatures, RLS, permission helpers, and private buckets against the portal's current generated Supabase types and deployed schema. Do not infer columns or enable writes from stale handoff documents.
2. Keep inventory mutations disabled until exact authenticated RPC contracts are verified. Never optimistically update local stock as though the server committed it.
3. Remove local role switching from operational builds. Keep local repository data clearly marked as prototype/demo; local inventory and shopfloor events do not sync to the portal or its audit trail.
4. Use only the authenticated Supabase Storage signed-URL path for approved private buckets. The local prototype signed-URL method now fails closed; do not replace it with a fabricated or public URL.
5. Verify sign-in, permission denial, field stripping, private-link expiry, and sync behavior before enabling shopfloor use. Do not create stock-affecting test records without approved fixtures.

## Build verification

From the Android project directory, run:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest assembleDebug
```

The latest verified run completed successfully with 26 unit tests passing. Re-run after Android source changes and report any compiler warnings separately from failures.
