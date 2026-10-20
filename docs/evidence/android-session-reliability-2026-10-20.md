# Android session and retry reliability

Functional increment only; no layout redesign.
- Shared reads/writes obtain a current access token; refresh is serialized before expiry using the existing Auth refresh endpoint. Tokens remain in encrypted storage. Refresh verifies returned user identity before saving. Rejected refresh invalidates local sign-in; transient network failure does not fabricate success.
- Late password-login and refresh responses cannot restore a session after sign-out/account change. Same-user token rotation does not reset current department navigation or trigger an account-cache change.
- Original dashboard refresh now loads the shared server workspace; returning to foreground reloads it too.
- Request identities are synchronously persisted in encrypted preferences before sending a shared mutation, keyed by account and payload digest. Uncertain retries reuse the ID after process restart. Confirmed mutations clear their ID. No payloads, offline posting queue, credentials or backend permissions are introduced.

Validation: testDebugUnitTest and assembleDebug passed with 41 unit tests. Expiry-boundary coverage added. Existing contract/permission tests remain passing. Real phone expiry, forced process-death retry and live refresh-token rejection acceptance are pending; compilation does not establish these results.

Remaining functional integration: record authoring/revision and review-request forms, stage-gate action editor, controlled private-file actions, and authoritative domain inventory/BOM/production projections and transactions. These require their exact verified web backend contracts. Current changes do not claim a full portal/mobile production release.
