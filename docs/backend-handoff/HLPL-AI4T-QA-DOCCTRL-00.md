# HLPL-AI4T-QA-DOCCTRL-00

**Status:** Working planning draft — review by Quality and department owners before operational use  
**Purpose:** Refine department-owned controlled documents and automatically maintain traceable audit records through the project lifecycle.  
**Scope:** SCMS portal document register, lifecycle deliverables, approval and change history, and related audit views for all 11 departments.

## 1. Objective and control principles

Make each project document traceable from its originating requirement through review, approval, release, change, supersession, retention, and archive. Automation may create reminders, tasks, and draft records; accountable people retain approval and release authority.

- One controlled document record has one owning department and may have multiple explicitly assigned reviewers/approvers.
- Every document is linked to its project (or marked as a corporate/common QMS document), lifecycle stage, and source/related records.
- Every state-changing event records who, when, what changed, why, and the affected document/revision. History is append-only in the shared operational system.
- A released revision is immutable. A change creates a new revision, captures change reason and impact, and routes required reviews again.
- A stage gate cannot close while its required deliverables, reviews, or evidence are incomplete or overdue, unless an authorized exception is recorded.
- Client views expose only approved, client-authorized deliverables; internal audit evidence, supplier/commercial data, and engineering risk content stay access-controlled.
- Local prototype and replaceable DEMO state remain separate from configured operational data until authenticated shared workflows are ready.

## 2. Department ownership and audit evidence

| Dept | Document owner / typical controlled evidence | Lifecycle audit events to maintain |
|---|---|---|
| SAL | Enquiry, customer SOR, NDA, feasibility/contract review, quotation, PO/customer communication | Intake source; customer requirement revision; feasibility routing and each HOD response; commercial approval; customer submission and acknowledgement |
| EHW | PRS inputs, schematics, PCB/BOM, hardware DFMEA, design review and release package | Requirement/design baseline; reviewer comments and disposition; design verification links; revision/change impact; released drawing/package checksum |
| ESW | Software requirements, architecture, source/build release, firmware test and OTA package | Build/version and source baseline; peer/security review; test evidence; release approval; deployment/rollback links |
| MECH | CAD/STEP, drawings, enclosure/mounting, material and tolerance evidence | Design revision; fit/DFM review; interface impact; released CAD/drawing package identity; deviation disposition |
| PUR | Approved supplier list, RFQ/quotation comparison, PO, supplier certificates and PPAP inputs | Supplier approval/expiry; sourcing decision; PO revision/approval; lot/certificate receipt; supplier audit action closure |
| QA | APQP, DVP&R, control-plan review, NCR/CAPA, audit reports, PPAP and compliance certificates | Plan approval; test execution/result/evidence; nonconformance and containment; corrective action verification; release decision and evidence completeness |
| EMF | Process flow, PFMEA/control plan inputs, work instructions, setup/parameter sheets | Process revision; readiness review; parameter change; training acknowledgement; production release and deviation records |
| TST | Test procedures, EOL/PDI results, calibration and equipment records | Procedure revision; unit/lot test result; failed test disposition/retest; equipment calibration status; final inspection release |
| FIN | Cost baseline, approved quotation references, tax invoice and payment/commercial records | Approval and source references; controlled invoice issue/correction; reconciliation and access-restricted audit events |
| SIT | Access/RBAC review, backup/restore evidence, service/RMA records, security configuration | Access grant/change/revoke; backup/restore result; incident and resolution; service issue trace; signed-link access event where policy requires |
| HR/OPS | Competency/training evidence, internal audit programme, legal and corporate QMS documents | Training assignment/completion; auditor independence/qualification; audit finding and closure verification; controlled policy distribution/acknowledgement |

For a cross-functional deliverable, retain exactly one accountable owner and record each participating department's role and review decision separately. Do not infer approval from a notification, file upload, or stage transition.

## 3. Controlled document lifecycle

`Planned → Draft → In Review → Approved → Released/Effective → Superseded → Obsolete/Archived`

1. **Plan:** Generate a required-document instance from the project stage and approved document-type checklist. Assign owner, required reviewers, due date, and applicable retention class. Missing inputs remain visible as blockers.
2. **Draft:** Create metadata using the naming standard `HLPL-[PROJ_ID]-[DEPT]-[DOC_TYPE]-[REV]`. Link the source requirement, related records, and template revision. Store working files as draft; do not present them as released evidence.
3. **Review:** Route to configured reviewers by document type, stage, and RACI. Each reviewer records `Approve`, `Reject`, or `Comment`, timestamp, identity, department, rationale, and decision on the exact revision. Rejection returns the draft to its owner with actions.
4. **Approval/release:** Validate required sign-offs, linked evidence, naming, revision, and completeness. Authorized approver releases the exact content hash. Record effective date and distribution scope; mark the prior released revision superseded only after successful release.
5. **Stage gate:** Evaluate required deliverables and accepted evidence. Auto-create handoff tasks and notifications for R/A/I roles. A gate decision records pass/hold/waiver, approver, rationale, and links to the evidence set.
6. **Change:** Any change to released content opens a change request with reason, affected requirements/parts/processes/departments, risk assessment, required revalidation, and proposed revision. Route impact reviews; preserve old content and approvals.
7. **Close/archive:** On project close or document obsolescence, verify open actions are resolved, preserve document and event history, set retention start/trigger, restrict access as required, and record archive disposition. Destruction requires an authorized, logged disposition event after the approved retention period.

## 4. Minimum data model

Use normalized entities instead of free-text history arrays for shared operational data:

- **document:** stable ID, document number/type, title, owner department, project/common scope, stage, classification, lifecycle state, current revision, effective/expiry dates, retention class, template version, creator, created/updated timestamps.
- **document_revision:** document ID, revision, immutable storage object/version, SHA-256 checksum, file metadata, change request ID, created by/at, release state, effective/superseded timestamps.
- **review_assignment / review_decision:** revision ID, reviewer and department, RACI role, due date, decision, rationale, decided by/at. One assignment per required reviewer/revision; no silent overwrite of decisions.
- **document_link:** typed relation to requirement, task, record, part, supplier, test, nonconformance, change request, or lifecycle gate.
- **audit_event:** event ID, organization/project, department, entity type/ID, revision, action, actor ID/role, timestamp, reason, before/after or field diff, correlation/transaction ID, source (user/automation), and relevant file checksum. Append-only with server-side authorization.
- **retention_rule / disposition:** record class, retention trigger/period, legal hold, disposition approval, completion evidence.
- **stage_deliverable_requirement:** stage + department + document type, required/conditional applicability, acceptance criteria, reviewer roles, due/trigger rule, and RACI mapping.

File bytes stay in private Cloud Storage. Database records hold metadata and object version/checksum; short-lived signed URLs are issued only after server-side authorization. Avoid permanent public URLs and client-side authorization as the security boundary.

## 5. Automatic event and audit matrix

| Trigger | Automatically maintained records | Human action still required |
|---|---|---|
| Project created / stage entered | Instantiate applicable document checklist, owner queue, due dates, stage event | Confirm scope/applicability and assign owner if unresolved |
| Draft created or edited | New version/checksum event, linked source records, editor and changed fields | Author confirms change reason and readiness for review |
| Submitted for review | Snapshot the submitted revision; create reviewer assignments/tasks; notify R/A roles | Reviewers decide and enter rationale |
| Review rejected / comments added | Append decision, preserve comments, reopen owner actions, recalculate gate status | Owner resolves comments and resubmits a new revision when content changes |
| Approved / released | Append signed approval and release event; record effective date and checksum; distribute to authorized roles | Authorized approver verifies evidence and releases |
| Change request opened | Impact-assessment task set by affected department; link impacted docs/records; hold affected gates where required | Owners assess impact; QA determines revalidation needs |
| Stage gate passed/held/waived | Snapshot evidence list and gate decision; create next-stage handoffs from RACI | Accountable gate owner records decision and waiver justification |
| Audit finding / NCR / CAPA opened | Link finding to relevant document revision, requirement and stage; due-date tasks and reminders | Department owner provides containment/correction; QA verifies effectiveness |
| Project closed / document superseded | Validate unresolved actions; apply retention class; archive index and disposition eligibility date | Owner and QA approve closure/disposition as configured |

Automated jobs must be idempotent (stable event/instance keys), retryable, and visible on failure. Notifications are not audit evidence; the underlying assignment, decision, or event is.

## 6. Department and lifecycle views

- **Department register:** owned docs, assigned reviews, due/overdue items, expiring evidence, open findings, and latest released revisions.
- **Project dossier:** stage-by-stage required deliverables, owner department, current revision/state, reviewer decisions, evidence links, gate status, and gaps.
- **Audit trail:** filters for project, department, document, revision, actor, action, and date; export a traceable evidence pack with event IDs/checksums and access record.
- **Quality oversight:** cross-department overdue reviews, audit finding/CAPA status, obsolete copies detected, gate exceptions, retention/legal holds.
- **Client view:** allowlisted released documents and permitted gate milestones only; never expose internal comments or unreleased revisions.

Every row should link to the record and revision that caused the event. Keep executive rollups derived from source events rather than separately editable counters.

## 7. Delivery plan

### Phase 0 — Control design and baseline

Quality approves document types, owners, reviewer rules, retention classes, stage checklists, naming examples, and exception authority. Reconcile the current document register with the 9-stage dossier and identify duplicate or missing document types. Mark all examples/demo records visibly and keep them replaceable.

### Phase 1 — Local prototype model and workflow

Extend the versioned browser-local portal state with structured revisions, review assignments/decisions, change requests, deliverable requirements, and typed audit events. Keep real/configured local records and replaceable DEMO data isolated. Add department and project dossier views. Use synthetic test flows only in DEMO state.

### Phase 2 — Shared persistence and authorization

Align one canonical Supabase schema with the application model; migrate existing records with stable IDs and an explicit legacy-history source marker. Enforce authenticated organization/department/project scope in RLS and server functions. Make audit insertion server-side append-only; add storage object version/checksum and signed access. Replace broad development policies before operational data is enabled.

### Phase 3 — Gate automation and audit evidence packs

Enable idempotent stage checklist creation, reminders, review routing, gate blockers, change impact tasks, and archive rules. Pilot one complete project with SAL, EHW/ESW, QA, PUR, EMF/TST, FIN, SIT, and HR/OPS participation as applicable. QA verifies event completeness and access boundaries before scaling.

## 8. Current implementation observations to resolve

- The main browser `AuditEvent` currently has only `id`, `at`, `action`, and `detail`; updates are kept to the latest 100 events. That is useful for prototype activity but not a durable audit ledger.
- The browser `ControlledDocument` stores revision/state and `history?: string[]`; it does not yet define structured per-revision approvals, exact content hashes, change requests, retention, or disposition.
- Department visibility logic includes ownership/reviewer heuristics and document keyword rules. Replace inference by title/URL with explicit owner and reviewer assignments before operational authorization depends on it.
- The legacy core migration's `controlled_documents` table and the authenticated storage functions' `documents` / `controlled_document_files` references need schema reconciliation. The legacy migration also contains permissive development policies; confirm the deployed current migrations and replace policies before production use.
- Existing roadmap/master-plan language describes immutable logs and automatic lifecycle behavior more strongly than the lightweight local event model currently provides. Keep those capabilities labeled as target behavior until server-enforced shared workflows are implemented and reviewed.

## 9. Acceptance criteria for operational release

1. A project stage generates the configured department deliverable checklist once, even after retries or refreshes.
2. Each released revision has a unique number, immutable file version/checksum, effective date, accountable approval, and retrievable prior revision.
3. Review, rejection, approval, release, change, gate, access-sensitive file action, and disposition events identify actor, time, target revision, reason, and source.
4. Department users see owned work and assigned reviews; clients see only explicitly released client documents; unauthorized access is rejected server-side.
5. A released-document change creates a linked change request and repeat review; old approvals cannot be reused for changed content.
6. Gate status is explainable from linked evidence and outstanding actions; overrides require role authority and recorded rationale.
7. Retention, legal hold, archive, and destruction decisions are explicit, reviewable, and traceable.
8. QA can reconstruct one document's full lifecycle and produce a project-stage evidence pack without relying on local browser history.

## 10. Governance note

This is a proposed system design, not a declaration of certification or automatic compliance. Quality Management must approve the applicable procedures, retention periods, access policy, and evidence acceptance criteria against the organization's controlled QMS and customer/statutory requirements before operational release.
