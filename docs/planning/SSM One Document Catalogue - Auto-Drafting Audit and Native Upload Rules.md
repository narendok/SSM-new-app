# SSM One Document Catalogue — Auto-Drafting, Audit, and Native Upload Rules

Refined: 19 October 2026. **Cross-platform document-control reference; not another implementation plan.**

The original 89 annexure item/note blocks are organized into **38 document/record families** with **90 family-to-source links** because source block B0545 maps to two families; D02 and D04 are clarification-only. A family is a logical group, not one mandatory file. These are coverage counts, not mandatory file counts. Unique subtypes and confidential scopes stay separate; no source requirement is discarded.

Use [the web implementation plan](Current%20Plans/Web%20Implementation%20Plan.md) for web delivery order. The full source-block mapping is in [the catalogue JSON](SSM%20One%20Document%20Catalogue%20-%20Auto-Drafting%20Audit%20and%20Native%20Upload%20Rules.json). The original source inventory is not included in this planning library.

## Confirmed master-plan alignment — 18–19 October 2026

This section aligns the existing 38-family reference with the confirmed master-plan decisions. It does not create 38 mandatory files, certify implementation, or add another delivery roadmap. The original 89-block coverage and stable family IDs are unchanged. New UI/workflow capabilities are mapped as overlays; not every feature becomes a DOCX template. Document families attach to lifecycle stages through the master plan's versioned responsibility matrix; this catalogue does not define or replace the stage model. Reusable-asset master, project configuration/reuse assessment, component/project certification evidence, and as-built genealogy are structured subrecords within existing families (D08, D10, D11, D13, D14, D23 and D31), not new family IDs or additional mandatory files. JSON records the same mapping under confirmed_master_plan_alignment and each affected family.

| Confirmed addition | Existing family IDs | Clarification |
|---|---|---|
| Template ownership and approval | D01 | Web-only scoped lead template editing; members fill/verify instances; authenticated Prepared by; lead review/return; configured CPO hardware/product, CTO software and CEO business/money routing; exact revision and separate release. |
| Customer onboarding and requirement intake | D02 D03 D04 D05 D08 | Pending customer registration with Sales/Finance reviews; separate progressive technical intake, assigned feasibility/conflicts and exact baseline acceptance. Customer form submission is not automatic activation or project start. |
| Research, visit testing and MOM | D13 D25 | Assigned R&D/Service mobile observations/readings/evidence generate reviewable report drafts. Service/complaint context remains distinct; not every visit is a nonconformance or DVP. Client-visible MOM is separately accepted/published with a notification; copy email subject/body only, manual sending. |
| People policies, forms and monthly reviews | D01 D27 D28 D29 | Web policy/template administration; mobile applicable policy reads, assigned forms, training and monthly self-review/feedback; leave/calendar, attendance/travel rules and reviewer scope remain contract-dependent. HR case privacy stays separate. |
| Timing matrix and reminders | D01 D27 D32 | Versioned configurable recurrence/event triggers, current assignees/reviewers, quiet hours, dedup and completion cancellation. Suggested defaults are organizational proposals, not IATF-mandated intervals. |
| Newsletter draft and common summaries | D38 D01 | Reviewable period summary rendition from permitted verified milestones, source pins and manual-edit preservation. No automatic external sending or invented achievements; no separate authority or mandatory document per project. |
| Client view, verification and feedback | D04 D13 D23 D25 D26 D38 | Published client project/order/service projection, company/individual isolation, required inputs, immediate eligible milestone feedback and independent support tickets. Scan/manual reference verifies permitted recorded device/document status, not physical authenticity. Keep internal costs/private remarks excluded. |
| Department and project Process Flow | D01 D21 D32 | Main lifecycle overview and lead Process Flow menu. Diagram configuration editing reuses the same protected settings contract; draft/validate/review/activate, history and gate checks. Production PFD is a distinct technical subtype, not the portal workflow editor. |
| Web/Android handoff | D01 | Separate web/backend master and Android build plan; explicit planned contract packet with caller scope, payloads, replay/version pins, receipts/readback and device acceptance. No mobile business backend or inferred installation. |

**Template decision:** authorized department leads maintain controlled web templates/header/footer and revisions; assigned members verify auto-filled source data and complete permitted fields. Preparation, lead review/return, applicable management approval and release remain separate exact-revision receipts. A signature image is not approval. Shared templates require explicit ownership; lead status does not itself grant template or executive-route authority.

**Drive placement:** each department can receive a task or document record through explicit assignment. Place the record by its business context, not by the assignee alone. Use the same four top-level groups in each department workspace:

1. **Common & Standards** — approved company/department standards, policies, SOPs, controlled blank templates, and the cross-department audit/QMS index and evidence records. It is the shared standards and audit area; use a configured standards owner rather than assuming a new “Standards” department exists. Organize records by owning department and audit scope. Keep one canonical record and link to it from reviews; do not make uncontrolled duplicate copies.
2. **Internal Projects** — internal company initiatives and project work, including their assigned tasks, decisions, and project-specific records.
3. **Client Projects** — external/customer project records, with the client and project access boundary applied.
4. **Department Operations & References** — non-project operational records and miscellaneous/reference material, including team knowledge base, approved reference links, recurring reviews, tickets, registers, and operational handoffs.

Project-specific evidence remains with its internal or client project, even when it is later reviewed for audit. The audit index in Common & Standards links to that canonical evidence, while common company/department standards and non-project audit records remain in the common area. Reuse one controlled template revision across instances. A policy, blank form, submitted response and completed review are different records, even within one family. Folder labels and task assignment do not grant access; ownership, explicit authorization, classification and confidentiality rules remain authoritative.

**Selected scope exclusions:** Google Drive/Docs/Sheets API/provider integration remains outside the selected implementation decision. No automatic email sending or mailbox-draft integration; MOM subject/content is copied for manual sending. No automatic publication, test pass, client acceptance, spend decision or project start. New client layout approval is a design baseline only. Reference template folders are still pending actual user-supplied access.

**Verification boundary:** this catalogue describes target behavior. Existing record APIs, template editors, private file persistence, source-calculation authority, ordinary employee/client/lead permissions, reminders, report generation and device flows require their own accepted contracts/evidence. The [web implementation plan](Current%20Plans/Web%20Implementation%20Plan.md) governs web sequencing; the separate Android build plan and planned handoff govern mobile implementation.

**Generation boundary and editing experience:** portal templates may prefill structured drafts, indexes and reports from authorized, source-linked project records. Users edit structured portal entries and can preview the populated template and download a DOCX or PDF rendition where listed; a full rich-text/in-app DOCX editor is not required. The downloaded file is a rendition, not a released source of truth; changes that need to be authoritative return through controlled revision and review. The tentative BOM is created in the portal from the controlled parts/inventory catalogue; it does not reserve or move stock. Native schematics, PCB/Gerber packages, firmware/software source and build artifacts, mechanical STEP/CAD/drawing files, actual lab/test reports and certificates are authored or issued by their specialist tools/providers and uploaded or linked as controlled files. The portal can generate their metadata index/cover/checklist, not the files themselves. This catalogue is the sole document-family, format, and generation-boundary reference; web and Android delivery steps remain in their existing platform plans.

## Current usable flow — prioritize these independently

| Family | Needed behavior | Format |
|---|---|---|
| Customer Register | Customer master and contacts refresh one Sales workbook | Existing records → genuine synced XLSX |
| Enquiries & Opportunities | One linked queue, owner, next action and review status | Software records |
| Master Specification / SOR | Customer inputs captured once with immutable source/revision | Versioned fields + downloadable DOCX/PDF rendition |
| Feasibility / contract review | Selected department/person reviews, evidence, conditions and notifications | Review records + DOCX rendition |
| Confirmed PRS / project start | Approved pinned baseline and explicit authorized project creation | Baseline record + DOCX |

NDA is required only when the configured customer/program calls for it. Quotation, sourcing, development, testing and production documents follow their applicable stage. Missing private DOCX/XLSX contracts block those saves; they do not make the entire catalogue a prerequisite for unrelated working features.

## Common pack and the four document groups

Each of the **12 departments in the initial registry** maintains its applicable SOPs, authority/roles matrix, KPI definitions and blank templates/work instructions under Common & Standards. Reuse controlled revisions; do not generate a new common pack for every project. The registry remains extensible as the organization changes. Engineering disciplines remain workstreams. The shared common area also supports department-wide audit/QMS indexing and evidence, organized by department and audit scope.

- **Common & Standards:** approved company/department standards, policies, templates and definitions, plus shared audit/QMS indexes and non-project audit evidence. Keep one canonical record; cross-link project evidence rather than copying it here.
- **Internal Projects / Client Projects:** actual applicable internal or customer project records/files, linked to the same source and revision. Explicitly assigned department tasks and records remain in the relevant project context.
- **Department Operations & References:** customer/supplier/personnel registers, recurring reviews, tickets, non-project operational work, and the approved references/knowledge base.

Periodic reviews are record types, not another top-level folder. PPAP is a conditional type; Starred is a shortcut. Existing protected folder mapping is preserved until a reviewed change; this catalogue defines target placement and does not itself move files.

## Department catalogue

All entries below are target/reference families. Actual readiness comes from saved records, file bytes and accepted contracts. Review frequency, owner assignments and requiredness are configured; source headings do not enforce them.

### Each department

| Document / record family | Format | Use / applicability |
|---|---|---|
| Common operating pack: SOPs, authority matrix and work instructions | DOCX / approved references | Maintain one controlled pack per department/workstream; reuse its revision across projects. Includes engineering/SDLC/CI standards, FIFO and nonconformance procedures. |

### Sales

| Document / record family | Format | Use / applicability |
|---|---|---|
| Customer Register and contacts | Software records → synced XLSX | One managed Customer Register.xlsx from Customer master; refresh additions/changes, retain revisions. Not one file per customer and not external Excel import. |
| Enquiries & Opportunities register / pipeline reviews | Software records / review log | One linked queue and stage/owner/review tracking; pipeline review uses the same records rather than duplicate forms. |
| Master Specification / customer inputs / SOR | Versioned fields + DOCX | Capture known requirements once, preserve source/customer/revision, reuse in feasibility and project start. SOR is a rendition, not another source of truth. |
| Feasibility / contract review | Assigned review records + DOCX | Selected departments record scope, owner, due date, evidence, decision and conditions; one consolidated review links the contributions. |
| Quotation / commercial proposal | DOCX / commercial records | When an opportunity requires a commercial offer; scoped Finance input, explicit issuance and version history. |

### Hardware / R&D

The following reusable-asset records are overlays within the existing catalogue families, not additional template families or required standalone files:

| Document / record family | Format | Use / applicability |
|---|---|---|
| PRS / confirmed requirement baseline | Versioned baseline + DOCX | After required feasibility/confirmation, pin the exact approved requirements for explicit project start and engineering. |
| Architecture / block diagram / HSI | Design files / specification | Applicable engineering scope; architecture and HSI remain distinct linked subtypes and exact source revisions. |
| Reusable asset master and release package (D10 subtype) | Controlled asset record + native files/evidence | Maintain one master identity per reusable PCB/assembly, harness, FW/SW component, mechanical part/tool/fixture, or process module. Record owner, revision/status, interfaces, compatibility/limitations, source links/checksums, verification/qualification evidence and effectivity. Attach any component/asset certificate to its exact covered revision and scope, with issuer, standard/edition, tested sample/conditions, limitations, and validity. Native files remain authored in specialist tools and uploaded/linked. |
| Design and release file package (D10) | Externally authored native CAD / schematic / PCB / ZIP uploads or controlled links | Engineer-provided schematic, PCB/Gerber/fabrication, firmware/build, enclosure/STEP, drawing and tooling files. The portal links immutable file revisions/checksums and may generate a cover/index; it does not generate or edit source files. |
| Project configuration and reuse assessment (D08/D11/D14 linked subrecord) | Structured project record; DOCX/PDF summary only where useful | Pin exact asset revisions to a project/product variant; map requirements/interfaces; record fit/gap, adaptations/deviations, impact/risk, delta verification, component certificate links and their applicability, project-level certification/test requirements, evidence, decision/approval and any tailored/waived activity rationale. Reuse or component certification alone never proves finished-product compliance or waives a gate/certification. |
| As-built configuration and asset genealogy (D11/D23 linked subrecord) | Build/serial/lot records + evidence links | Record the released asset revisions and applicable component/firmware/software/harness/enclosure/tooling identities actually used by each build/serial/lot; link substitutions, production checks, nonconformance and change effectivity. |
| Engineering BOM / cost tracker | Portal-owned structured BOM + XLSX/CSV rendition | Create the tentative BOM by selecting parts from the controlled catalogue/inventory and entering project quantities/designators. Link each line to the reusable asset/configuration revision, item master, approved alternate, and supplier/lot controls as applicable. Procurement reviews the same pinned revision. Creating/exporting a tentative BOM does not reserve/move stock or create a purchase; the released baseline requires configured approval. |
| DFMEA | Structured ratings/actions + DOCX | When design risk analysis is applicable; ratings, actions and closure come from actual engineers/reviewers. |
| POC / design verification / DVP&R | Structured test plan/results + uploaded external report/evidence; DOCX/PDF rendition where useful | At applicable POC/validation stages; keep planned checks distinct from actual measured results and externally issued lab reports; link source/firmware/sample revisions. Never generate a test pass or certificate. |
| Design change / ECO / ECN review | Change register + evidence | On a change or failed review; preserve previous decisions and record impact/effectivity. Stage/ECO audits read this history. |
| Software quality / security / performance review | Review records | Only firmware/software/cloud scopes that need these reviews; frequency is configured, not imposed on every project. |
| DRC / component-library review | Review records | When hardware/library review is due or change-triggered; reference actual rules and evidence. |

**Inventory record boundary:** inventory balances and movements remain operational software records, not duplicate documents. Stock-controlled items include electronics components/accepted outsourced PCB assemblies, mechanical raw materials and purchased parts, configured WIP/subassemblies, consumables/MRO, and packaging. Physical production machinery, test equipment, fixtures, tooling, calibration instruments, and facility equipment use the fixed-asset register. Revisioned PCB/case/harness designs and similar reusable modules are engineering assets, not on-hand quantities; their BOMs link to exact stock item IDs and approved sourcing. The [Web Implementation Plan](Current%20Plans/Web%20Implementation%20Plan.md) defines inventory/asset handling; receipts, inspections, issues, returns, adjustments, and maintenance keep their own authorized records and audit history.

### Procurement

| Document / record family | Format | Use / applicability |
|---|---|---|
| Supplier qualification / ASL / ACML / performance and capacity | Supplier registers + review evidence | One supplier history with evaluation, on-site/system reviews, approved lists, scorecards/capacity/lead-time observations. No duplicate workbook per review title. |
| Supplier PPAP / submission warrant | Checklist + supplier evidence | Only applicable customer/product/supplier programs; a document-type/project category, never a fifth department Drive group. |

### Production

Apply these families to the master plan's two manufacturing departments: **EMF** owns accepted outsourced PCBA receiving/traceability, line assembly/box-build, controlled firmware loading and production functional checks; **MMF** owns mechanical fabrication, finishing, subassembly and inspection. QA retains independent quality criteria and disposition. TST/EOL is not a separate current department; tests are assigned to the relevant process and authority. In-house bare-board fabrication and PCBA assembly remain out of scope.

| Document / record family | Format | Use / applicability |
|---|---|---|
| Tooling condition / custody / mold maintenance review | Asset/tool review records | Production maintains process tools; Engineering supplies design evidence; Facility handles custody where authorized. |
| Calibration / MSA / expiration review | Instrument register + evidence | Applicable instruments/gages/processes; Operations quality audit links the same authorized calibration evidence. |
| Line capacity / startup / process flow | Readiness records / PFD | When preparing an actual build/line; use recorded capacity and readiness, retain separate process-flow subtype. |
| PFMEA / control plan | Distinct structured templates / DOCX | Applicable manufacturing process; PFMEA and control plan retain their own fields/decisions and cross-references. |
| EOL / FQC / PDI | Measured unit records + uploaded evidence; DOCX/PDF rendition where useful | For actual build/dispatch units; serial, firmware, test/inspector evidence and explicit disposition, never generated passes. |
| 5S / OEE / yield / scrap / defects / KPI reviews | Operational registers + calculated views | When production observations/reviews exist; use defined formulas/time windows. Standards/templates are Common & Standards; completed non-project reviews are Department Operations & References, while project-specific evidence stays with its project and is linked from audit views. |
| Process-equipment PM / downtime / OEE | Maintenance and performance records | For applicable production equipment and actual maintenance/observations; Facility supports infrastructure separately. |

### Operations

| Document / record family | Format | Use / applicability |
|---|---|---|
| NCR / 8D / CAPA / lessons learned | Linked case and action records | When nonconformance or learning occurs; originating departments retain evidence and Operations coordinates the case. |
| Linked risk registers / project risk matrix | Scoped risk records | Combine repeated enterprise-risk titles into one index, with separate authorized strategic, financial, infrastructure/security and project scopes. No blanket shared confidential register. |
| Certification / statutory applicability and validity evidence (D31) | Applicability record + authentic external report/certificate | Keep component/asset certification attached to the exact PCB/assembly, enclosure/material, harness, or other covered revision; separately record project/finished-product certification for the exact combined configuration, customer, market, and applicable authority/lab scope. Link standards/editions, issuer, ID, test sample/configuration/conditions, limitations, validity and authentic source file. Component evidence may support, but never substitutes for, finished-product tests/approval. ISO/IATF/licenses/consents/NOC references are not automatically mandatory templates or claimed certificates. |
| APQP / internal audits / governance / management review | Assigned checklist/review records | Applicable program/governance reviews; one schedule/history links scope, customer feedback, risks and decisions across the current departments (12 in the initial registry). |
| DMAIC / improvement charter and tollgate evidence | Improvement project records | Only approved improvement work; real sign-offs/evidence, not a compulsory document for every client project. |
| Cross-department KPI definitions / dashboard | Definitions + authorized aggregate views | Reuse measured records and the reviewed formula from each department, not a second manually copied enterprise tracker. |

### Operations / Legal

| Document / record family | Format | Use / applicability |
|---|---|---|
| NDA and confidential agreement | DOCX + actual agreement evidence | Only when required by the customer/program. Template belongs to Legal/Common; executed agreement is scoped to its case/project. Sales initiates/links, without Legal access. |

### Finance

| Document / record family | Format | Use / applicability |
|---|---|---|
| Receivables / MIS / financial KPI / closing / billing | Financial records + XLSX/DOCX renditions | Use Finance-owned records; Sales sees permitted commercial context. Tax invoice is a separate authorized rendition. IT tickets from the combined source note stay outside Finance records. |

### HR

| Document / record family | Format | Use / applicability |
|---|---|---|
| Skill matrix / TNI / training / manpower and workforce reviews | Restricted HR records + matrix rendition | Recorded competence/training/assignment; corporate scope unless project-linked work is needed. License governance is linked to Operations, not another HR workbook. |
| Onboarding / BGV | Restricted policy and case records | When an authorized onboarding/verification case exists; policy is Common, personnel evidence remains restricted. |
| Grievance / employee query | Restricted case register | Only actual authorized employee cases; not a generally shared project document. |

### Facility / Maintenance

| Document / record family | Format | Use / applicability |
|---|---|---|
| Utilities / DG / electricity and water | Facility records + evidence | Actual utilities/assets/consumption/maintenance, with scoped Finance payment review; unknown source shorthand requires field configuration. |
| IT backup / security review / project IT tickets | Authorized IT records | Infrastructure work only; privileged configuration remains administrator-controlled. Split project IT tickets from financial billing in the source note. |
| Facility/security service / waste disposal / pest control | Contracts, service and disposal evidence | Applicable services/assets/schedules; actual agreements/qualified disposal evidence and private commercial terms. Short reminders are work tasks, not templates. |

## Avoid duplicate documents and unnecessary work

Software registers, reviews, tasks, notifications, PR/RFQ/PO/GRN, work orders, releases and change cases remain authoritative software records. Create a file only for a useful controlled output or export. A reminder, dashboard, source note or certification reference is not automatically a new DOCX template.

Merged families preserve their meaningful subtypes: PRS and HSI; schematic/PCB/mechanical files; DFMEA and PFMEA; control plan and PDI; individual review contributions and the consolidated decision. Grouping never merges private Finance/IT/HR/Legal contents or overwrites revisions.

Legacy HLPL/SSM codes are source aliases, not instructions to renumber records. Official IDs come from the server. Certification/standards/license references stay conditional and unverified until real applicable evidence exists.

The web master plan already defines pilot sourcing, release/production/dispatch, change loops, audit and backend handoff. This catalogue does not repeat delivery packs or create another approval checklist. Evidence belongs in `docs/evidence`; technical contracts remain separate. Android builds are governed by their own plan.

Source coverage checked: **89 of 89 unique annexure blocks mapped across 90 family-source links**. D02 and D04 reflect later clarifications and are not additional mandatory files. The source-block IDs and family mapping are retained in the JSON; the separate original source inventory is not included in this library. No live records, grants, policies, templates or application storage paths changed by this planning update.

