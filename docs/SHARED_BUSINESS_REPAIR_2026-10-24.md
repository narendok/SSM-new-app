# Android shared business repair — 24 October 2026

Baseline: `5d10316`. The repository still targets the existing SSM-new backend; no endpoint or credential was changed.

- Removed the synthetic authenticated-session fallback after unsuccessful sign-in. Cached demo/synthetic tokens cannot authenticate TEST operations.
- Added SharedBusinessRepository using the same guarded get_business_workspace / mutate_business_workspace RPC contract as web. A save requires the server's saved:true response. Stable request IDs survive retries.
- Shared business screens participate in real expense requests/review/posting, tickets, employee assignments, messages and polls. TEST entry points use these screens, while existing local examples remain separate.
- Clearing/authentication errors and account changes invalidate business snapshots. Legacy local financial/chat mutations reject authenticated TEST use.
- Existing native layout/navigation and engineering/inventory screens were retained. Advanced HR authoring remains on web.

Verification: `testDebugUnitTest assembleDebug` succeeded; XML reports contain 54 tests, zero failures, zero errors. Three new tests cover failed reads without sample substitution, shared mutation identity/request IDs, and rejection of unconfirmed saves. This is compile/unit evidence, not a phone or web-to-phone round trip.

Backend migrations are maintained in SSM-new-web. The first business migration was deployed through Lovable on 21 October. The follow-up 20261024084915 validation migration awaits fresh deployment confirmation as of this report; check the web verification report for its final deployment status.

No templates, signatures, real employee facts, budget thresholds or production financial data were invented. Private letters/assets require their original controlled record contracts. Recurring delivery, advanced builder features, attachment integration and device acceptance still have the explicit limits recorded in the web repair report.
