# Backend Gaps and Pending Contract Verification Report

## Overview
This document summarizes open gaps and pending backend contract verifications required before live data execution and stock mutations can be enabled on the Android mobile application.

## 1. Project Environment & Project ID Alignment
- **Required Target URL**: `https://yyrvduosyyaluifqvwkr.supabase.co`
- **Required Project ID**: `yyrvduosyyaluifqvwkr`
- **Gap / Action Item**: Confirm that all legacy portal configurations or local dev references are pointing strictly to `yyrvduosyyaluifqvwkr` and that RLS policies are deployed on this target environment.

## 2. Google Workspace OAuth Provider Setup in Supabase
- **Redirect URI**: `com.example.myapplication://auth-callback`
- **Flow**: PKCE (`S256`)
- **Gap / Action Item**: Ensure Google OAuth Client ID and Redirect URIs are configured in the Supabase Dashboard under `Auth -> Providers -> Google`.

## 3. Unverified Safe View Schemas (`PENDING_LIVE_SCHEMA`)
The following safe views are defined as query boundaries in the mobile app, but exact live column names and data types must be confirmed by the database team:
1. `operational_receiving_purchase_orders`
2. `operational_receiving_purchase_order_items`
3. `operational_purchase_requests`
4. `operational_purchase_request_items`
5. `operational_rfqs`
6. `operational_rfq_vendors`
7. `operational_po_shipments`
8. `operational_vendors`
9. `lifecycle_read_data`
10. `document_control_registers`
11. `controlled_document_audit_timeline`

- **Action Item**: Provide OpenAPI / Supabase schema dump for these 11 views so mobile DTOs can be bound without guessing columns.

## 4. Unverified RPC Signatures for Stock Mutations
The following planned RPC calls remain disabled on mobile:
1. `create_grn`
2. `material_issue`
3. `material_return`
4. `material_transfer`
5. `material_adjustment`

- **Action Item**: Verify exact live RPC parameter names, data types, idempotency header handling, and return payload structures for `create_grn` and other stock operations.
