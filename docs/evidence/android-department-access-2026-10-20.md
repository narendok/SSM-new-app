# Department navigation and account access correction

Reviewed and fast-forwarded planning commit a66eefc, including mobile companion boundaries, 14-entity scope, department presentation aliases and server authorization. Section 9.9 describes an older commit's offline fake-auth fallback; that historical behavior was removed by subsequent verified auth fixes and is not current authority.

Corrections:
- Department navigation opens a department-view picker, never the TEST sign-in chooser. Account sign-in remains under Account. Admin may filter existing server-returned departments; leads/members receive their resolved department; viewers receive no internal department picker. Selection cannot alter auth credentials or RPC authority. The backend registry supplies department IDs and names.
- Authenticated dashboard uses server membership display name, projects, tasks and approval counts instead of the common seeded command center. Employee names/roles are no longer replaced with a department lead persona in its identity card. Admin filtering does not expand the server response. Empty and failed reads remain explicit.
- Task action controls require assigned employee, owning department lead or admin; approval controls require explicitly requested reviewer department lead or admin. Backend transactions independently recheck all authority and stale revisions.
- Authenticated domain tabs without verified integration display an unavailable-contract message instead of exposing mutable local simulation under a signed-in operational identity. Unauthenticated prototype remains separate. No schema, RLS or backend permissions changed.

Validation: 40 Android unit tests passed and assembleDebug succeeded. Three new access tests cover admin/lead/member/viewer scopes, unknown department fail-closed behavior, assigned versus colleague tasks, wrong department writes, designated-reviewer versus requester and immutable completed decisions. Fourteen real-PostgreSQL backend contract tests passed, covering department isolation, employee writes, delegated reviews, direct write denial, revision binding and atomic gates.

Limits: physical device/UI acceptance is pending user testing. Full inventory, BOM, private file, controlled document and background sync integration remain pending. Existing live TEST backend round-trip evidence predates this presentation correction; no new users, permissions or live records were changed in this increment. This is not a full mobile release certification.
