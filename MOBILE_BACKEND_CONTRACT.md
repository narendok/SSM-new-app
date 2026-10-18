# Mobile Backend Contract Specification

## 1. Environment & Target Backend
- **Endpoint**: `https://yyrvduosyyaluifqvwkr.supabase.co`
- **Target Project ID**: `yyrvduosyyaluifqvwkr`
- **Auth Provider**: Supabase Auth (Google Workspace OAuth PKCE `S256`)
- **Callback Scheme**: `com.example.myapplication://auth-callback`

## 2. Safe Read-Only Views
All mobile view queries must strictly target the following views. Do not query underlying base tables. Exact column schemas are marked `PENDING_LIVE_SCHEMA`.

### View Endpoints
1. `/rest/v1/operational_receiving_purchase_orders`
   - Schema: `PENDING_LIVE_SCHEMA`
2. `/rest/v1/operational_receiving_purchase_order_items`
   - Schema: `PENDING_LIVE_SCHEMA`
3. `/rest/v1/operational_purchase_requests`
   - Schema: `PENDING_LIVE_SCHEMA`
4. `/rest/v1/operational_purchase_request_items`
   - Schema: `PENDING_LIVE_SCHEMA`
5. `/rest/v1/operational_rfqs`
   - Schema: `PENDING_LIVE_SCHEMA`
6. `/rest/v1/operational_rfq_vendors`
   - Schema: `PENDING_LIVE_SCHEMA`
7. `/rest/v1/operational_po_shipments`
   - Schema: `PENDING_LIVE_SCHEMA`
8. `/rest/v1/operational_vendors`
   - Schema: `PENDING_LIVE_SCHEMA`
9. `/rest/v1/lifecycle_read_data`
   - Schema: `PENDING_LIVE_SCHEMA`
10. `/rest/v1/document_control_registers`
    - Schema: `PENDING_LIVE_SCHEMA`
11. `/rest/v1/controlled_document_audit_timeline`
    - Schema: `PENDING_LIVE_SCHEMA`

## 3. Stock Mutation RPC Contracts (Guarded)
Stock mutations are disabled on mobile until exact live signatures are provided and verified.

1. `rpc/create_grn`
   - Status: Disabled
   - Signature: `PENDING_LIVE_SCHEMA`
   - Client Idempotency: Persistent Client UUID across retries
2. `rpc/material_issue`
   - Status: Disabled
   - Signature: `PENDING_LIVE_SCHEMA`
3. `rpc/material_return`
   - Status: Disabled
   - Signature: `PENDING_LIVE_SCHEMA`
4. `rpc/material_transfer`
   - Status: Disabled
   - Signature: `PENDING_LIVE_SCHEMA`
5. `rpc/material_adjustment`
   - Status: Disabled
   - Signature: `PENDING_LIVE_SCHEMA`

## 4. Private Storage Contract
- Endpoint: `POST /storage/v1/object/sign/{bucket}/{filepath}`
- Body: `{ "expiresIn": 300 }`
- Public URL Generation: Prohibited
