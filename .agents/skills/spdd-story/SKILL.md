---
id: spdd-story
category: Development
description: Decompose high-level feature requirements into INVEST-compliant, business-focused stories with clear scope boundaries and testable acceptance criteria
---

Decompose high-level feature requirements into structured, business-focused development stories. Each story is independently deliverable, scoped to 1-5 days of work, and includes concrete acceptance criteria written in business language. Stories are split according to the INVEST principle and serve as input for the SPDD workflow (`/spdd-analysis` → `/spdd-reasons-canvas` → `/spdd-generate`). This command focuses on the **"What"** and **"For whom"** — technical analysis and implementation details are left to downstream SPDD phases.

**Input**: The argument after `/spdd-story` is a business requirement description or file reference.

Input can be provided in two ways:

1. **Text description**: Direct text describing the feature or requirement
2. **File/folder reference**: Using `@` to reference files or folders containing requirements

**Examples**:

```
# File reference
/spdd-story @requirements/team-management-feature.md

# Text description
/spdd-story Implement team management API with CRUD operations, member management, and role-based access control

# Combined
/spdd-story @requirements/team-management.md additionally needs invitation flow and audit logging
```

**Steps**

1. **Validate and consolidate business input**

   a. **If no input provided**, use the **AskUserQuestion tool** (open-ended, no preset options) to ask:
   > "Please provide the business requirement document or description for story generation (you can use text, @file references, or both)."

   **IMPORTANT**: Do NOT proceed without business input.

   b. **If input contains `@` file/folder references**:
    - Read ALL referenced files completely using the Read tool
    - For folder references, read all relevant files within the folder
    - Consolidate all file contents into a unified business context

   c. **Combine all context sources**:
    - Merge text descriptions with file contents
    - Preserve the complete information from all sources — do NOT summarize or truncate

   d. **Context Integrity Check**:
    - Verify all `@` references were successfully read
    - If any file cannot be read, report the error and ask user to provide alternative
    - Confirm the consolidated context contains sufficient information to proceed

2. **Scan existing epics and stories for context**

   Do NOT explore the codebase — that is `/spdd-analysis`'s responsibility. Only gather enough context to avoid duplication and determine numbering.

   a. **List existing epics and stories**:
    - List files in `requirements/` directory (if exists)
    - Read existing stories to understand numbering conventions and scope boundaries

   b. **Identify coverage gaps**:
    - Note which features and operations are already covered by existing stories
    - Ensure new stories do not duplicate or conflict with existing ones

   c. **Determine the next identifier using the naming convention**:
    - Identify top-level epic files named `Epic-N-{kebab-case-title}.md`.
    - Identify child story files named `Story-MMM-SSS-{kebab-case-title}.md`, where `MMM` is the story module and `SSS` is the sequence within that module.
    - For a new top-level epic, assign `N` as one greater than the highest existing Epic number.
    - When decomposing an epic, assign the next available module `MMM` and start its child stories at sequence `001`.
    - Do not use a global file count for child-story filenames, and do not name an epic with a `Story` prefix.

3. **Abstract task analysis and INVEST evaluation**

   Before creating any stories, analyze the feature at an abstract level.

   a. **Identify the abstract task**:

   ```
   ### Abstract Task: "[Feature Name]"

   **Analysis Dimensions**:
   - **Core Responsibility**: [What is the primary purpose of this feature]
   - **Primary Operations**: [List the main operations: create, query, update, delete, list, etc.]
   - **Key Constraints**: [Data uniqueness, permissions, associations, business rules]
   - **Technical Complexity**: [Low/Medium/High — standard CRUD, complex business logic, etc.]
   - **Business Complexity**: [Low/Medium/High — multiple query scenarios, cascading effects, etc.]
   ```

   b. **If splitting is needed**, determine the split strategy:

   **Split dimensions** (choose the most appropriate):
    - **By operation type**: CREATE-READ / UPDATE-DELETE / LIST-SEARCH
    - **By complexity**: Basic features / Advanced features / Admin features
    - **By user role**: Regular user features / Admin features
    - **By technical dependency**: Core features / Extension features

   **Split rules**:
    - Each story contains at most 2-3 core functional points
    - Functional points within a story must have logical relevance
    - Single story workload: 1-5 days
    - Each story must deliver independent business value
    - All epics MUST produce something demonstrable to the user; each story SHOULD ideally be demonstrable too

4. **Determine epic and story module numbering**

   a. **Scan existing files** in `requirements/` to distinguish `Epic-N` files from `Story-MMM-SSS` files.
   b. **For a top-level epic**, assign the next Epic number `{EPIC_NUMBER}` (for example, `Epic-3`).
   c. **For stories decomposed from an epic**, assign the next story module `{MODULE_NUMBER}` (for example, `003`) and reference the parent as `Epic-{EPIC_NUMBER}`.
   d. **Assign sequential story numbers** within that module: `001`, `002`, `003`, etc.
   e. **Keep the identifiers aligned**: the child heading uses `STORY-{MODULE}-{SEQ}`, while its filename uses `Story-{MODULE}-{SEQ}`.

5. **Generate stories following the standard structure**

   For each story determined in the split analysis, generate the complete story using this structure:

   ---

   ### Epic and Story Title Formats

   Top-level epic documents use the Epic identifier in both the filename and the document heading:

   ```markdown
   # Epic: Epic-{N} {Epic Description}
   ## Epic-{N} {Epic Description}
   ```

   Child story documents use the story module/sequence identifier in the filename and the STORY identifier in the document heading:

   ```
   ## STORY-{MODULE}-{SEQ} {Story Title}
   ```

   Do not use `STORY-001-{N}` inside an epic document. Do not use a global sequential number such as `Story-25` for a child story when its module/sequence identifier is available.

   ### Required Sections

   #### Section 1: Background

   ```markdown
   ### Background
   [Describe the business background, use cases, and role within the overall system]

   Key points:
   - Business value and user needs
   - Relationship with other features
   - Why this capability is needed now
   ```

   #### Section 2: Business Value

   ```markdown
   ### Business Value
   - Provide {specific capability} for {role}
   - Support {specific need} in {business scenario}
   - Enable {key function} of {system goal}
   ```

   #### Section 3: Dependencies and Assumptions

   ```markdown
   ### Dependencies and Assumptions
   - **Prerequisites**: [Features or stories that must be completed first, if any]
   - **Data assumptions**: [What data or entities are expected to already exist]
   - **Integration points**: [External systems, APIs, or services this story interacts with]
   - **Business constraints**: [Regulatory, contractual, or organizational constraints]
   ```

   #### Section 4: Scope In / Scope Out

   ```markdown
   ### Scope In
   - [Feature included in this story, bullet points]

   ### Scope Out
   - [Feature NOT included in this story, bullet points]
   ```

   #### Section 5: Acceptance Criteria (ACs)

   Generate business-focused ACs using Given-When-Then format. The number of ACs depends on the business scenarios — do NOT use a fixed number. Cover these categories as appropriate:

   **a. Happy path ACs** — Core business scenarios with concrete examples:
   ```markdown
   #### AC{N}: {Business Scenario Description}
   **Given** {business precondition with concrete values/examples}
   **When** {user action in business language}
   **Then** {expected business outcome with specific numbers}
   ```

   **b. Validation and business rule ACs** — Input validation and business constraints, expressed as user-facing behavior:
   ```markdown
   #### AC{N}: {Validation Scenario}
   **Given** {invalid or edge-case input condition}
   **When** {user attempts the action}
   **Then** {system rejects with clear user-facing message}
   ```

   **c. Error condition ACs** — Expected failure scenarios from the user's perspective:
   ```markdown
   #### AC{N}: {Error Scenario}
   **Given** {condition that causes failure, e.g., resource does not exist}
   **When** {user attempts the action}
   **Then** {system responds with appropriate error and HTTP status}
   ```

   **d. Non-functional expectations** (optional, only if relevant to the business):
   ```markdown
   #### Non-Functional Expectations
   - [Business-level quality expectation, e.g., "Billing calculation must complete fast enough for real-time API response"]
   - [Business-level quality expectation, e.g., "System must handle concurrent usage submissions from the same customer correctly"]
   ```

   **AC writing guidelines**:
    - Use **business language**, not implementation language (say "system rejects the request" not "return HTTP 400")
    - Include **concrete numbers and examples** (say "100,000 monthly quota, 80,000 used" not "some quota, some usage")
    - Exception: HTTP status codes ARE acceptable in ACs since they are part of the API contract visible to consumers
    - Each AC must be **independently testable** by a QA engineer without reading source code
    - Do NOT prescribe HOW to implement (no "use parameterized queries", "apply cache strategy", etc.)
    - Do NOT specify internal technical details (no JSON response format, no DB schema, no specific error codes)

6. **Quality check each generated story**

   After generating each story, verify against this checklist:

   **Structure and Completeness**:
    - [ ] Contains all required sections (Background, Business Value, Dependencies and Assumptions, Scope In/Out, ACs)
    - [ ] Each AC uses Given-When-Then format with concrete values and examples
    - [ ] ACs are written in business language — no implementation details leaked in
    - [ ] ACs cover happy path, validation/business rules, and error conditions

   **Business Clarity**:
    - [ ] Business value is clear and stated for a specific audience/role
    - [ ] Scope In and Scope Out clearly delineate boundaries — no ambiguous overlap
    - [ ] No duplication with other existing stories
    - [ ] A QA engineer could write test cases from the ACs without reading source code

   **Sizing and Independence**:
    - [ ] Story contains at most 2-3 core functional points
    - [ ] Story can be developed and delivered independently

7. **Final INVEST re-validation**

   After all stories are generated, perform a final check:

   **For each story**:
    - [ ] **Independent**: Does not depend on other stories' implementation details
    - [ ] **Complete**: Contains a complete functional loop and acceptance criteria
    - [ ] **Valuable**: Delivers independent business value when implemented alone
    - [ ] **Estimable**: Development team can accurately estimate development time
    - [ ] **Right-sized**: 1-5 day workload, at most 3 core functional points
    - [ ] **Testable**: Has clear test scenarios and acceptance conditions

   **Anti-patterns to avoid**:
    - Do NOT split by technical layer (frontend/backend/database for the same feature)
    - Do NOT over-fragment a single API into multiple stories
    - Do NOT break business logic completeness
    - Do NOT create complex inter-story dependencies

8. **Assemble and save the story document**

   a. **Construct the complete document**:

   The Story Decomposition section (INVEST analysis + split strategy) is placed in the **epic file only**. Each child story file contains just the story itself.

   **Epic file** — contains the top-level epic content followed by the Story Decomposition:

   ```markdown
   # Epic: Epic-{N} {Feature Name}

   ## Epic-{N} {Feature Name}

   ### Background
   [Epic background and business context]

   ### Business Value
   [Epic-level business value]

   ### Dependencies and Assumptions
   [Epic-level dependencies, assumptions, and constraints]

   ### Scope In
   [Epic-level scope included]

   ### Scope Out
   [Epic-level scope excluded]

   ### Acceptance Criteria
   [Epic-level acceptance criteria]

   ## Story Decomposition

   ### Abstract Task: "{Feature Name}"
   **Analysis Dimensions**:
   - **Core Responsibility**: ...
   - **Primary Operations**: ...
   - **Key Constraints**: ...
   - **Technical Complexity**: ...
   - **Business Complexity**: ...

   **Conclusion**: Needs splitting

   ### Split Strategy
   **Dimension**: ...

   | Story | Title | Core Functional Points |
   |-------|-------|----------------------|
   ```

   **Child story file** — contains only the story itself, no repeated decomposition:

   ```markdown
   ## STORY-{MODULE}-{SEQ} {Story Title}

   ### Background
   ...

   ### Business Value
   ...

   ### Dependencies and Assumptions
   ...

   ### Scope In / Scope Out
   ...

   ### Acceptance Criteria
   ...
   ```

   b. **Derive the file name according to artifact type**:
    - **Epic:** `Epic-{N}-{kebab-case-title}.md`, where `N` is the next available Epic number.
    - **Child story:** `Story-{MODULE}-{SEQ}-{kebab-case-title}.md`, where `MODULE` and `SEQ` match the story heading's `STORY-{MODULE}-{SEQ}` identifier.
    - **Parent reference:** Child stories must link to the parent `Epic-{N}` filename and must not repeat a legacy `STORY-001-{N}` identifier as the parent reference.

   The command MUST generate ONE file per story. No consolidated files are allowed, even when stories are closely related, because the downstream SPDD workflow processes one story at a time and because per-story files keep acceptance criteria, scope boundaries, and change history independent.

   For each story, create a separate file:
    - `Story-{MODULE}-001-{story-1-title}.md`
    - `Story-{MODULE}-002-{story-2-title}.md`
    - etc.

   The epic file is saved as a single `Epic-{N}-{kebab-case-title}.md` containing the epic-level content plus the full Story Decomposition. Child story files do not repeat the decomposition.

   c. **Create directory and write file(s)**:
    - Ensure directory `requirements/` exists under the project root (create if not)
    - Write the complete story document(s) to `requirements/<file-name>.md`

   d. **Show summary to user**:

   ```
   ✅ Story generation complete. Stories saved to `requirements/`

   📋 Generation summary:
    - Feature: Epic-{N} {feature name}
    - Stories generated: [count]
    - Total ACs: [count]

    📝 Stories:
    1. STORY-{MODULE}-001 {title} — `Story-{MODULE}-001-...md`
    2. STORY-{MODULE}-002 {title} — `Story-{MODULE}-002-...md`
   ...

   🔗 Next step: Process each story through the SPDD workflow:
      /spdd-analysis @requirements/<file-name>.md
   ```

9. **Offer to proceed with SPDD analysis**

   > "Stories are ready. Would you like me to proceed with `/spdd-analysis` for any of the generated stories?"

   If the user selects a story, invoke the `/spdd-analysis` workflow with that story file as input.

**Output**

Structured, INVEST-compliant epic or story document(s) saved to `requirements/`, containing:
- Abstract task analysis and split strategy
- Complete stories with Background, Business Value, Dependencies and Assumptions, Scope In/Out, and business-focused Acceptance Criteria
- Quality-checked against business clarity, sizing, and independence checklists

**Guardrails**

- Do NOT proceed without business requirement input
- Do NOT explore the codebase — codebase analysis is `/spdd-analysis`'s responsibility
- Do NOT summarize or truncate the original business requirement — preserve it verbatim when referenced
- Do NOT generate code — this command produces stories only
- Do NOT leave placeholders or TODO items — generate complete, specific content
- Do NOT modify any existing files in the codebase
- Do NOT split stories by technical layer (frontend/backend/database)
- Do NOT create stories with complex inter-story dependencies
- Do NOT over-fragment a single API endpoint into multiple stories
- Do NOT specify HOW to implement — only WHAT the expected behavior is
- Do NOT prescribe technical solutions in ACs (no caching strategies, indexing approaches, query patterns, error JSON formats, P95/P99 metrics)
- Do NOT include security implementation details in ACs (no "use parameterized queries", "sanitize HTML", etc.) — those are `/spdd-reasons-canvas` Safeguards
- ACs MUST use business language that a QA engineer or Product Owner can understand
- ACs MUST include concrete numbers, examples, and expected outcomes
- Each story MUST pass INVEST compliance checks
- Each story's workload MUST be between 1-5 days
- Each story MUST contain at most 3 core functional points
- All epics MUST produce something demonstrable to the user; each story SHOULD ideally be demonstrable too
- Always read ALL `@` referenced files completely
- Always create `requirements/` directory if it does not exist
    - File names MUST follow the artifact-specific naming convention:
        - **Epics:** `Epic-{N}-{kebab-case-title}.md`
        - **Child stories:** `Story-{MODULE}-{SEQ}-{kebab-case-title}.md`
    - Epic headings MUST use `Epic-{N}`, not `STORY-001-{N}`.
    - Child-story parent references MUST link to `Epic-{N}` and must not repeat a legacy parent STORY identifier.

**Context Integrity Guardrails**:

- **MUST read ALL `@` referenced files completely** — do NOT skip or partially read any referenced file
- **MUST read folder contents** when `@` references a folder — scan and read all relevant files
- **Do NOT summarize or truncate** referenced file contents — preserve full information
- **Verify all references resolved** — if any `@` reference fails to read, report error immediately
- **Combine all sources** — merge text descriptions with file contents into unified context
- **Preserve original intent** — do not interpret or modify the meaning of provided context

**Integration with SPDD Workflow**

This command is the **story decomposition phase** of the SPDD workflow, transforming high-level feature requirements into structured, implementable stories that feed into the SPDD pipeline:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           SPDD Workflow                                  │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  Pre-Phase: /spdd-story    ← YOU ARE HERE                              │
│  ┌────────────────────────────────────────────────────────────────┐    │
 │  │ High-Level Feature Requirement                                  │    │
 │  │   + Abstract Task Analysis (dimensions + split strategy)        │    │
│  │   + Story Decomposition (2-3 functional points per story)       │    │
│  │   + Business-Focused ACs (Given-When-Then with concrete data)   │    │
│  │   = Structured, Business-Ready Stories                           │    │
│  │                                                                  │    │
 │  │ Output: requirements/Epic-N-*.md or                            │    │
 │  │         requirements/Story-MMM-SSS-*.md                   │    │
│  └────────────────────────────────────────────────────────────────┘    │
│                              │                                          │
│                    ┌─────────┴─────────┐                                │
│                    │  For each story:   │                                │
│                    └─────────┬─────────┘                                │
│                              ▼                                          │
│  Phase 0: /spdd-analysis                                                │
│  ┌────────────────────────────────────────────────────────────────┐    │
│  │ Story → Enriched Context (domain concepts + strategy + risks)  │    │
│  │                                                                 │    │
 │  │ Output: spdd/analysis/GGQPA-XXX-*-Analysis-*.md              │    │
│  └────────────────────────────────────────────────────────────────┘    │
│                              │                                          │
│                              ▼                                          │
│  Phase 1: /spdd-reasons-canvas                                         │
│  ┌────────────────────────────────────────────────────────────────┐    │
│  │ Enriched Context → REASONS Canvas Structured Prompt             │    │
│  │                                                                 │    │
│  │ Output: spdd/prompt/GGQPA-XXX-*.md (REASONS Canvas)           │    │
│  └────────────────────────────────────────────────────────────────┘    │
│                              │                                          │
│                              ▼                                          │
│  Phase 2: /spdd-generate                                               │
│  ┌────────────────────────────────────────────────────────────────┐    │
│  │ Structured Prompt → Validate → Generate → Verify → Code        │    │
│  └────────────────────────────────────────────────────────────────┘    │
│                              │                                          │
│                              ▼                                          │
│  Phase 3: /spdd-sync                                                   │
│  ┌────────────────────────────────────────────────────────────────┐    │
│  │ Code Changes → Analyze → Update Prompt → Consistency           │    │
│  └────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

**Why This Phase Matters**

High-level feature requirements often describe complex capabilities that cannot be implemented in a single sprint. `/spdd-story` bridges this gap by:

1. **Right-sizing work**: INVEST analysis ensures each story is independently deliverable within 1-5 days
2. **Business clarity**: ACs use concrete examples and business language that POs, QA, and developers all understand
3. **Reducing rework**: Stories with clear scope boundaries and acceptance criteria minimize ambiguity during implementation
4. **Enabling parallelism**: Independent stories can be developed concurrently by different team members
5. **Feeding the SPDD pipeline**: Each generated story is immediately ready for `/spdd-analysis`, creating a smooth end-to-end workflow

**Separation of Concerns across the SPDD Pipeline**:

| Concern | `/spdd-story` | `/spdd-analysis` | `/spdd-reasons-canvas` |
|---------|---------------|-------------------|------------------------|
| Audience | PO, Scrum Master, QA | Dev Team, Architect | Dev Team |
| Language | Business language | Strategic / conceptual | Technical / implementation |
| Focus | "What to build & for whom" | "What & Why in codebase context" | "How to build it" |
| Scope | Feature decomposition, story boundaries | Domain concepts, design direction, risks | Entity models, architecture, operations |
| ACs | Business scenarios with concrete data | AC coverage & gap analysis | Safeguards with specific metrics |
| Technical depth | None — out of scope | Strategic (trade-offs, approach) | Full (code structure, error formats, P99) |
| Codebase access | None (only reads `requirements/`) | Deep concept-driven exploration | Full exploration for implementation |
| Output | `requirements/Epic-N-*.md` or `requirements/Story-MMM-SSS-*.md` | `spdd/analysis/*-Analysis-*.md` | `spdd/prompt/*.md` (REASONS Canvas) |
