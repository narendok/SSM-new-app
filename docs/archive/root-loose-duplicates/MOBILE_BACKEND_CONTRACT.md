# Mobile Backend Contract Specification

## 1. Environment & Target Backend Authority
- **Endpoint**: `https://rzskhndweihuczbdhoxk.supabase.co`
- **Target Project ID**: `rzskhndweihuczbdhoxk`
- **Auth Provider**: Supabase Auth (Google Workspace OAuth PKCE `S256`)
- **Callback Scheme**: `com.example.myapplication://auth-callback`

## 2. Operational APQP Stage Gate Advancement Contract (Batch 3/4 Parity)
- **Authoritative RPC**: `public.verify_and_advance_stage_gate(p_record_id uuid, p_target_stage integer, p_comments text, p_evidence jsonb)`
- **Session Identity**: Derived inside database transaction from `auth.uid()` via `resolve_operational_membership()` (caller-supplied IDs denied).
- **Stage 2 Feasibility Freeze**: Requires approved Contract Review Record (`HLPL/F/MKT/01`) before advancing to Stage 3.
- **Stage 3 Requirements Baseline Freeze**: Requires approved domain requirement sheets across all 5 engineering domains (`EHW-PRS`, `FW-FRS`, `SW-SRS`, `MECH-MRS`, `SAL-QUO`) or approved N/A rationales.
- **Stage 4 Schematics Prerequisite**: Requires approved `EHW-SCH-01` before advancing to Stage 5+.
- **Concurrency & Idempotency**: Row locking `FOR UPDATE`; identical duplicate invocations return idempotent success without duplicate audits.

## 3. Safe Read-Only Views
All mobile view queries must strictly target safe views or trusted RPC endpoints. Do not query underlying base tables directly.

### Safe Views
1. `/rest/v1/operational_receiving_purchase_orders`
2. `/rest/v1/operational_receiving_purchase_order_items`
3. `/rest/v1/operational_purchase_requests`
4. `/rest/v1/operational_purchase_request_items`
5. `/rest/v1/operational_rfqs`
6. `/rest/v1/operational_rfq_vendors`
7. `/rest/v1/operational_po_shipments`
8. `/rest/v1/operational_vendors`
9. `/rest/v1/lifecycle_read_data`
10. `/rest/v1/document_control_registers`
11. `/rest/v1/controlled_document_audit_timeline`

## 4. Private Attachment Vault Contract
- **Endpoint**: `POST /storage/v1/object/sign/{bucket}/{filepath}`
- **Body**: `{ "expiresIn": 60 }`
- **Approved Buckets**: `controlled-documents`, `project-drive`, `pcb-photos`, `datasheet-cache`, `external-staging`
- **Expiry Policy**: Strict 60-second signed URL expiry enforced.
- **Public URL Generation**: Strictly prohibited.
