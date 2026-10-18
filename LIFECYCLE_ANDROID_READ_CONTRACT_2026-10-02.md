# Lifecycle Read Data Android Contract (2026-10-02)

## 1. Scope
This contract specifies the read-only view boundary for entity lifecycle tracking within the SCMS Mobile App.

## 2. Targeted Safe View
- **View Identifier**: `lifecycle_read_data`
- **REST Endpoint**: `/rest/v1/lifecycle_read_data`
- **Access Level**: Read-Only via RLS. No direct table queries permitted.

## 3. Schema Status
- **Column Schema**: `PENDING_LIVE_SCHEMA`
- **Verification Status**: Unverified against live database. No fabricated columns or guessed data types exist in mobile client models.

## 4. Mobile Execution Policy
- Queries on `lifecycle_read_data` are handled through `SafeViewsRepository`.
- While schema status is `PENDING_LIVE_SCHEMA`, repository methods explicitly return `NotImplementedError` or schema unverified status to prevent execution of unverified queries.
- Results cached locally with timestamp (`lastSyncedTimestamp`) once live queries are enabled.
