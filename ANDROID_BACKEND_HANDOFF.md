# Android Mobile App to Backend Handoff Document

## Overview
This document details the security model, backend boundary configuration, and required contract verifications for the SCMS Android Mobile Application.

## Target Environment
* **Target Supabase URL**: `https://rzskhndweihuczbdhoxk.supabase.co`
* **Target Project ID**: `rzskhndweihuczbdhoxk`
* **Target Environment**: Production backend authority (Lovable backend). The legacy staging project `yyrvduosyyaluifqvwkr` is retired per owner directive.
* **Environment Configuration**: Verified via `local.properties` (ignored from git) and enforced by build validation in `app/build.gradle.kts`.

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
4. **Authorization & Actor Derivation**:
   - Server Row-Level Security (RLS) is authoritative.
   - Server RPC `verify_and_advance_stage_gate` derives the caller identity from authenticated session `auth.uid()` via `resolve_operational_membership()`, denying caller-supplied actor IDs.

## Operational APQP Stage Gate Advancement Contract (Batch 3/4 Parity)
| Gate | Rule / Prerequisite | Enforcement |
| :--- | :--- | :--- |
| **Stage 2** | Feasibility Freeze | Requires approved Contract Review Record (`HLPL/F/MKT/01`) before unlocking Stage 3 |
| **Stage 3** | Requirements Baseline Freeze | Requires approved domain requirement sheets across all 5 engineering/commercial domains (`EHW-PRS`, `FW-FRS`, `SW-SRS`, `MECH-MRS`, `SAL-QUO`) or approved N/A rationales |
| **Stage 4** | Hardware Schematics Prerequisite | Requires approved `EHW-SCH-01` before advancing to Stage 5+ |
| **Any** | Concurrency & Idempotency | Row locking `FOR UPDATE` on backend; duplicate retries return idempotent success; stage regression is permitted |

## Storage & Document Workflow
* **Private Attachment Vault**: Private file access uses `/storage/v1/object/sign/<bucket>/<path>` with strict 60-second signed URL expiry.
* **Approved Private Buckets**: `controlled-documents`, `project-drive`, `pcb-photos`, `datasheet-cache`, `external-staging`.
* **Public Storage URLs**: Strictly disabled across all mobile endpoints.
* **Integrity**: Files are bound to organization, department, project, document and exact revision with cryptographic SHA-256 validation.
