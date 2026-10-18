# Android Mobile App to Backend Handoff Document

## Overview
This document details the security model, backend boundary configuration, and required contract verifications for the SCMS Android Mobile Application.

## Target Environment
* **Target Supabase URL**: `https://yyrvduosyyaluifqvwkr.supabase.co`
* **Target Project ID**: `yyrvduosyyaluifqvwkr`
* **Target Environment**: Production backend only (No Dev/UAT targets permitted for mobile production build).
* **Environment Mismatch Note**: Note potential project ID mismatch between legacy local/portal references and the required production backend `yyrvduosyyaluifqvwkr`. All mobile traffic is bound strictly to `yyrvduosyyaluifqvwkr`.

## Security & Auth Boundary
1. **Credentials Management**:
   - `SUPABASE_URL` and `SUPABASE_ANON_KEY` are supplied via `local.properties` into `BuildConfig`.
   - No service-role keys, database passwords, or JWT secrets exist in mobile code or artifacts.
2. **Keystore Storage**:
   - Android Keystore-backed `EncryptedSharedPreferences` (`AES256_GCM` / `AES256_SIV`) is used exclusively for token and idempotency storage.
   - Plain `SharedPreferences` fallback is disabled. Unavailability causes closed failure (`SecurityException`).
3. **Google Workspace PKCE SSO**:
   - OAuth 2.0 PKCE flow (`S256`) is triggered via system browser.
   - Deep link redirect: `com.example.myapplication://auth-callback`.
   - No client secrets are embedded in the APK.
4. **Authorization**:
   - Zero local boolean role overrides. Server Row-Level Security (RLS) is authoritative for all data access.

## Safe Views Status (Read-Only)
| Safe View Name | Boundary Status | Column Schema Verification |
| :--- | :--- | :--- |
| `operational_receiving_purchase_orders` | Boundary Defined (Unimplemented) | `PENDING_LIVE_SCHEMA` |
| `operational_receiving_purchase_order_items` | Boundary Defined (Unimplemented) | `PENDING_LIVE_SCHEMA` |
| `operational_purchase_requests` | Boundary Defined (Unimplemented) | `PENDING_LIVE_SCHEMA` |
| `operational_purchase_request_items` | Boundary Defined (Unimplemented) | `PENDING_LIVE_SCHEMA` |
| `operational_rfqs` | Boundary Defined (Unimplemented) | `PENDING_LIVE_SCHEMA` |
| `operational_rfq_vendors` | Boundary Defined (Unimplemented) | `PENDING_LIVE_SCHEMA` |
| `operational_po_shipments` | Boundary Defined (Unimplemented) | `PENDING_LIVE_SCHEMA` |
| `operational_vendors` | Boundary Defined (Unimplemented) | `PENDING_LIVE_SCHEMA` |
| `lifecycle_read_data` | Boundary Defined (Unimplemented) | `PENDING_LIVE_SCHEMA` |
| `document_control_registers` | Boundary Defined (Unimplemented) | `PENDING_LIVE_SCHEMA` |
| `controlled_document_audit_timeline` | Boundary Defined (Unimplemented) | `PENDING_LIVE_SCHEMA` |

*Note*: Direct queries to sensitive base tables (finance, base purchase orders, salaries) are strictly forbidden.

## Planned Stock Mutation RPCs (Disabled)
| Planned RPC | Status | Persistent UUID | Signature Verification |
| :--- | :--- | :--- | :--- |
| `create_grn` | Disabled | Client UUID Generated | `PENDING_LIVE_SCHEMA` |
| `material_issue` | Disabled | Client UUID Generated | `PENDING_LIVE_SCHEMA` |
| `material_return` | Disabled | Client UUID Generated | `PENDING_LIVE_SCHEMA` |
| `material_transfer` | Disabled | Client UUID Generated | `PENDING_LIVE_SCHEMA` |
| `material_adjustment` | Disabled | Client UUID Generated | `PENDING_LIVE_SCHEMA` |

## Storage & Document Workflow
* **Private Storage Access**: Private file access uses `/storage/v1/object/sign/<bucket>/<path>` (expires in 300s). Public storage URLs are disabled.
* **Document State Transitions**: Disabled on mobile; UI explicitly directs users to the Web Portal for workflow transitions.
