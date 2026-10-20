# ANDROID BACKEND HANDOFF

## Safe configuration

Provide the Android project only these non-secret values through the Android build environment:

- `SUPABASE_URL`
- `SUPABASE_ANON_OR_PUBLISHABLE_KEY`

Do not add any service key, database password, signing secret, or third-party credential to Android.

## Environment mapping

The SSM-new-web Portal & Wizard project is configured for Supabase project rzskhndweihuczbdhoxk at https://rzskhndweihuczbdhoxk.supabase.co. Android Gradle pins SUPABASE_URL to this value and rejects a mismatch; its ignored local.properties contains the same URL and the portal publishable key. This resolves URL identity only: live schema, RLS, auth, storage authorization, and mobile data synchronization remain unverified. DEV/UAT/PROD separation is **not evidenced**. Do not add another backend authority.

## App flow

1. Sign in with the existing managed authentication provider.
2. Resolve the signed-in identity through `profiles`, staff linkage through `employees`, and capabilities through RLS/governed permission tables.
3. Query only RLS-permitted records with the publishable client.
4. Use the documented authenticated server mutation/RPC contracts for controlled actions.
5. For private files, request authorized signed delivery/upload flows; never build public object URLs.

## Stage 5 controlled inventory actions

- Android posts Final GRN only through `create_grn` with a UUID generated once per submission and persisted across retries. The same UUID and unchanged payload return the original result; a changed payload is rejected.
- Material issue, return, transfer, and adjustment use their respective protected posting contracts. They are online-only, server-numbered, and never composed from client-side table writes.
- Use only `operational_receiving_purchase_orders` and `operational_receiving_purchase_order_items` for receiving screens. These operational projections deliberately omit unit prices, totals, tax, margins, payment terms, vendor banking, and finance-only data.

## RLS assumptions

- Every mobile request is made with the current user session.
- RLS is authoritative; an empty result may mean access is not granted.
- Never replace an RLS check with locally cached role state.
- Re-check access and state when an offline draft is submitted.

## Storage pattern

Private buckets remain private. Download delivery uses an authorized signed URL when needed. External staged files use a server-issued signed upload token and remain quarantined until reviewed.
