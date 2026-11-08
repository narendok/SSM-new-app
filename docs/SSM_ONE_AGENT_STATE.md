# SSM One Android — Agent State

Latest backend contract audit: 2026-10-31. Historical implementation checkpoint: 2026-10-03.

## Current Stage 5 contract override — 2026-10-31

A read-only Lovable audit/preflight reported all five Stage 5 stock RPCs and both receiving views absent from Android's confirmed TEST target `rzskhndweihuczbdhoxk`. It also reported all 13 prerequisite procurement/inventory base tables and four permission/numbering helper functions absent, so archived Stage 5 SQL is not self-contained and cannot be applied as-is. The separate connected Supabase account cannot administer that project; the historical handoff ref `yyrvduosyyaluifqvwkr` is stale. Android stock mutation methods are disabled; do not build calls against inferred/archive-only contracts. Migration versions `20261029120000` and `20261031152000` are absent from the Supabase ledger but recorded as Drizzle rows 16/17; `20260924154357` was absent from both checked ledgers. These ledger results are from the confirmed target. Never hand-edit a ledger. See `backend-handoff/mobile_backend_contract.json`.

## Final-framework implementation checkpoint — 2026-10-03

Follow-on Drive increment: [category implementation report](evidence/department-drive-categories-2026-10-03.md).
Android now filters Common and Project Deliverables using verified root kinds;
Reviews explicitly reports missing backend mapping. Compile, **79 tests**, build
and in-place emulator update passed. Latest APK SHA-256:
`707b1b80f489ea21c0acfd4c34b90a0f58fb2327fb8cc1974a7da2f8a6341f31`.
Common/Projects/Reviews and HR scope reset were checked on the emulator. Lovable
reports a read-only `/admin/drive-ownership` preflight and an UNAPPLIED review
mapping proposal, with 12 focused tests/typecheck/build passing. It has not
reassigned legacy ownership or deployed the proposed review mapping.
Independent admin preview checks found and led to repairs of child-route
rendering and FILE/revision gap counting. The served preflight now shows 8
unmapped projects, 9 missing Drive revisions and 2 missing BOM source/revision
links. This is metadata visibility, not byte retrieval or non-admin acceptance.

- Final user DOCX and all three planning artefacts were uploaded to the existing Lovable project. Source-only Pack 1 work was dispatched and reported complete; the full lifecycle target is not complete.
- Android project Documents now uses the shared department Drive with both project and department scope, including request/history/file-open invalidation on scope changes. Removed the older unscoped engineering Documents entry point.
- Historical procurement line-read implementation targeted `operational_receiving_purchase_order_items`; the 2026-10-31 audit reported this view absent in the checked TEST backend, so live procurement reads are not accepted until the target and deployed view are re-verified.
- Read-only lifecycle stages now show pinned template/version references and both legacy and multi-predecessor dependencies, using the exported October 2 contract. No lifecycle action RPC is enabled.
- Compile, 76 unit tests (zero failures/errors/skips), debug build and emulator update passed. Original debug signer was reused; login/data preserved. Final APK SHA-256: `552863694ec90c2f26b88253eddd760f5b2614032f89b4b5d7dce10273bdb83a`.
- Emulator verified operational PO/part/vendors, project Documents entry point and HR-specific Work/More presentation. Admin-selected departments do not establish least-privilege acceptance.
- Lovable reports a read-only scoped lifecycle template register at `/settings/lifecycle`, correct three-category Drive presentation and BOM-query restrictions for excluded departments even when a project is selected. Source tests/build are reported evidence, not independent database acceptance.
- Emulator_V2.0 project and Drive nodes have NULL department ownership; its saved BOM lacks source Drive node/revision. No historical ownership or file provenance was guessed or changed. Periodic/conditional review folder mapping remains absent.
- Protected lifecycle action deployment and DB rollback/concurrency/non-admin RLS acceptance remain blocked by missing isolated test support and approved identities. Preserve all existing roles/departments and RLS.

Current evidence: [implementation report](evidence/final-framework-implementation-2026-10-03.md), [actual rendered web contract](backend-handoff/LIFECYCLE_ANDROID_READ_CONTRACT_RENDERED_2026-10-03.txt), and [legacy Drive mapping](evidence/lovable-legacy-drive-mapping-verification-2026-10-03.txt).

The historical September checkpoint below remains dated evidence, not the current instruction to submit a live GRN.

## Stage 5 status

The 2026-09-27 entries below are historical implementation/device evidence only and are superseded for backend readiness by the 2026-10-31 audit above.

- 5A Procurement read-only: UI/repository implementation is not proof of current deployed view availability; live view check reported absent.
- 5B Receiving Draft: local draft capture is separate from server posting; final GRN RPC is absent in the checked backend.
- 5C Inventory read-only: prior local/runtime checks do not establish current target identity or broad RLS acceptance.
- 5D Material Issue / Return: Android RPC methods remain disabled; the live functions were reported absent.
- 5E Cycle Count / Picking: blocked pending a verified backend contract.
- 5F GRN, stock adjustment and stock transfer: Android RPC methods remain disabled; the live functions were reported absent. No live posting accepted.
- 5G: prior My Day/scanner/task/notification checks are historical and do not close cross-platform acceptance.

## Current controlled next action

Do not perform a live stock-changing acceptance yet. First verify the backend project identity, deploy the reviewed contract through its supported migration workflow, and pass live signature/grant/RLS/idempotency/concurrency preflight. Only then schedule final GRN acceptance against a dedicated approved test PO.
## Latest verification — 2026-09-27

- The dedicated Android Sync Test Vendor PO `PO-2026-0001` is visible in Receiving as DRAFT with one VNH7040AYTR line and 1 remaining.
- The final GRN view clearly identifies its server-controlled transaction and requires invoice number, ISO date, a valid positive receipt, and a review step.
- A malformed test entry left the Review final GRN control disabled. The unsaved app state was discarded; no server transaction was invoked and the backend PO remains unchanged.
- Compile, unit test, and debug APK build gate completed without test failures.

- Repeated UAT form-entry checks left Review final GRN disabled when the required receipt fields were not valid; all unsaved form state was discarded and no GRN RPC was called.
