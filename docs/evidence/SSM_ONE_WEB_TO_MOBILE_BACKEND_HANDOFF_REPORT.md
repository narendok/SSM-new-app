# SSM One Web to Mobile Backend Handoff Report

## Executive Summary
This report documents the architectural setup, security boundaries, and handoff state of the SCMS Android Mobile Application. The mobile app has been configured to interface strictly with the production Supabase backend target `https://yyrvduosyyaluifqvwkr.supabase.co` (`yyrvduosyyaluifqvwkr`).

## 1. Security & Storage Code Review (Backend Still Unverified)
- **Android Keystore Enforcement**: Verified that `SecureStorage` relies strictly on `EncryptedSharedPreferences` backed by the Android Keystore. Plain `SharedPreferences` fallbacks are prohibited and removed.
- **Credential Hygiene**: Verified that `SUPABASE_URL` and `SUPABASE_ANON_KEY` are the only configuration properties loaded from `local.properties`. Service-role keys, database passwords, and JWT secrets are absent.
- **PKCE SSO**: Google Workspace OAuth flow utilizes PKCE (`S256`) with browser callback `com.example.myapplication://auth-callback`. No OAuth client secrets are embedded in the mobile binary.
- **Authorization boundary**: The client has no local role booleans. Authenticated requests attach bearer tokens; live RLS policies and server-side permission checks have not been verified.

## 2. Safe View Boundaries & Schema Status
The mobile client names 11 intended data sources but does not query them until live schema verification. Their exact names and projection safety remain unverified:
- `operational_receiving_purchase_orders`
- `operational_receiving_purchase_order_items`
- `operational_purchase_requests`
- `operational_purchase_request_items`
- `operational_rfqs`
- `operational_rfq_vendors`
- `operational_po_shipments`
- `operational_vendors`
- `lifecycle_read_data`
- `document_control_registers`
- `controlled_document_audit_timeline`

*Schema Status*: Marked `PENDING_LIVE_SCHEMA`. Repository implementations return explicit `NotImplementedError` or unverified status to ensure zero fake or unverified database queries are executed.

## 3. Stock Mutation Guarding
Planned stock operations (`create_grn`, `material_issue`, `material_return`, `material_transfer`, `material_adjustment`) generate and persist a Client UUID in Keystore storage for idempotency across retries. Actual RPC network calls remain disabled, returning `UnsupportedOperationException` until live RPC signatures are verified with the backend team. Stock mutation actions and screens are omitted from the UI.

## 4. Private Storage & Workflow Boundaries
- **Signed Storage URLs**: Private files are accessed via `/storage/v1/object/sign/<bucket>/<path>` (300s expiration). Public URLs are strictly forbidden.
- **Document State Transitions**: Workflow state transitions and document approvals stay disabled on mobile; users are directed to the Web Portal.

## 5. Verification & Build Confirmation
- **Gradle Sync & Build**: Executed `gradle_sync` and `app:assembleDebug`. Build status: SUCCESS.
- **Backend Verification Claim**: The backend contract is explicitly labeled UNVERIFIED pending live database schema confirmation from the backend team.

