# AI Work Rules — Road Guide

## Mandatory Instructions for All AI Agents

**Read this file before starting ANY task on Road Guide.**

---

## Rule 1: Knowledge Base First

> **Before any task, read `docs/project-knowledge/PROJECT_INDEX.md`**
> Then read task-relevant documents from the knowledge base.
> Do NOT re-read the full 1800-line `ROAD GUIDE.txt` unless resolving a specific contradiction.

**Entry Points by Task Type**:
| Task | Read First |
|------|------------|
| Architecture change | `02_SYSTEM_ARCHITECTURE.md` + `12_TECHNICAL_DECISIONS.md` |
| Database/schema | `03_DATA_MODEL_AND_DATABASE.md` |
| GPS/Tracking | `04_GPS_AND_TRACKING.md` |
| Exposure formulas | `05_EXPOSURE_ENGINE_AND_FORMULAS.md` |
| API integration | `06_ENVIRONMENTAL_DATA_AND_INTEGRATIONS.md` + `09_API_AND_DATA_CONTRACTS.md` |
| Routing/Map | `07_ROUTING_AND_MAPS.md` |
| UI/Design | `08_UI_UX_AND_DESIGN_SYSTEM.md` + `design/figma-export/FIGMA_EXPORT_INDEX.md` |
| Background service | `10_BACKGROUND_SERVICES_AND_PERMISSIONS.md` |
| Testing | `11_TESTING_AND_QUALITY.md` |
| Current status | `13_CURRENT_IMPLEMENTATION_STATUS.md` |

---

## Rule 2: Verify in Code, Not Docs

> Documentation describes **intent**. Source code is **truth**.

- If doc says "implemented" but code doesn't exist → doc is wrong
- If code behaves differently from doc → code wins, update doc
- Never assume a class/method/constant exists — `grep` first

---

## Rule 3: Three States — Never Conflate

Every feature/statement must be labeled:

| Label | Meaning |
|-------|---------|
| ✅ **Implemented** | Code exists, compiles, tested |
| 🟡 **Partially Implemented** | Some code, gaps documented |
| 📋 **Planned** | Spec only, no code |

**Never** present Planned as Implemented.
**Never** hide gaps behind "partially implemented" without details.

---

## Rule 4: Constants Are Sacred

> **Do not change these values without explicit audit + tests:**

| Constant | Value | Location |
|----------|-------|----------|
| `TIME_REFERENCE_SECONDS` | 1800.0 | `ExposureConstants` |
| `EXPOSURE_T_MAX` | 2.0 | `ExposureConstants` |
| `EXPOSURE_ROAD_SOURCE_FACTOR` | 1.0 | `ExposureConstants` |
| `EXPOSURE_CALIBRATION_K` | 5.0 | `ExposureConstants` |
| `DEFAULT_SKIN_FACTOR` | 1.0 | `ExposureConstants` |
| `ACTIVITY_FACTOR_STATIONARY` | 0.7 | `ExposureConstants` |
| `MAX_ENV_DATA_AGE_HOURS` | 3 | `EnvironmentalValidator` |
| `MAX_COORD_DISTANCE_KM` | 30 | `EnvironmentalValidator` |
| `MAX_FUTURE_OFFSET_MINUTES` | 15 | `EnvironmentalValidator` |

**Gradle, AGP, Kotlin, SDK versions** — same rule: no changes without audit.

---

## Rule 5: No Medical Claims

> Exposure Score = **relative estimate (0–100)**, not measured dose.

**Forbidden Language**:
- ❌ "measured dose", "actual exposure", "inhaled amount"
- ❌ "health risk", "medical risk", "dangerous"
- ❌ μg/m³ as user-facing output (internal only)
- ❌ "safe" / "unsafe" based on score

**Required Language**:
- ✅ "Estimated Exposure", "Environmental Exposure Score"
- ✅ "Data Confidence", "Coverage"
- ✅ "Relative to your other days/routes"
- ✅ Disclaimer in Settings → Environmental Preferences

---

## Rule 6: Missing Env Data ≠ Zero Pollution

> **Never** substitute 0 for missing environmental data.

**Correct Behavior**:
- Mark `DataQuality = OUT_OF_RANGE` / `STALE` / `MISSING`
- Reduce `Confidence` and `Coverage`
- Show "Incomplete Data" / "Data Unavailable" in UI
- Exposure Score = "Incomplete" or partial with warning

---

## Rule 7: Privacy First

| Requirement | Implementation |
|-------------|----------------|
| Local storage default | Room only, no cloud sync |
| No mandatory auth | All core features work offline |
| Private zones | Geofence + data exclusion |
| Data export/delete | User-initiated, complete |
| No analytics SDK | v1: zero third-party tracking |

---

## Rule 8: Architecture Changes Require Dependency Analysis

> Before modifying any component, check:
> - What uses it? (grep for imports)
> - What does it use? (constructor params, imports)
> - Tests affected?
> - Migration needed?

**Document in `12_TECHNICAL_DECISIONS.md`** with new ADR.

---

## Rule 9: Update Knowledge Base After Significant Changes

After completing a task that changes architecture, formulas, APIs, or data model:

1. Update affected `.md` files in `docs/project-knowledge/`
2. Update `13_CURRENT_IMPLEMENTATION_STATUS.md`
3. Add ADR to `12_TECHNICAL_DECISIONS.md` if architectural
4. Keep `00_PROJECT_INDEX.md` links current

---

## Rule 10: Use `CURRENT_IMPLEMENTATION_STATUS.md` as Reality Check

Before starting work:
- Check component status in `13_CURRENT_IMPLEMENTATION_STATUS.md`
- If 📋 Planned → implement from scratch
- If 🟡 Partial → understand gaps first
- If ✅ Implemented → verify in code, don't reimplement

**But**: Always verify critical details in actual code — status file may be stale.

---

## Rule 11: Figma Assets via Export, Not Guessing

> UI implementation must reference `design/figma-export/`

- Colors, spacing, icons from exported tokens
- Screen layouts from exported frames
- If asset missing → check `FIGMA_EXPORT_INDEX.md` for gaps
- Never invent design values

---

## Rule 12: Test Before Claiming Done

| Change Type | Minimum Verification |
|-------------|---------------------|
| New function | Unit test + lint pass |
| Repository method | Integration test (Room) |
| API client | MockWebServer test |
| Engine formula | Golden test case |
| UI screen | Compose preview + device test |
| Background service | Manual device test (scenarios in `11_TESTING_AND_QUALITY.md`) |

**Never** claim "done" without `./gradlew test lint ktlintCheck` passing.

---

## Rule 13: Contradictions → Document, Don't Guess

If you find:
- Spec vs Code mismatch
- Doc vs Doc mismatch
- Spec vs Spec (different sections) mismatch

**Action**:
1. Document in `12_TECHNICAL_DECISIONS.md` (new ADR or update)
2. Note in relevant `.md` file with ⚠️
3. Ask human for resolution if ambiguous
4. **Do not** silently pick one

---

## Rule 14: No Secrets in Repo

> API keys, tokens, passwords — **never** commit.

- Use `local.properties` or environment variables
- Figma token → not in repo (export manually or via CI secret)
- Google Maps API key (if ever used) → `local.properties`

---

## Rule 15: Commit Messages Follow Convention

```
<type>(<scope>): <subject>

<body>

<footer>
```

| Type | Use For |
|------|---------|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation only |
| `refactor` | Code change, no behavior change |
| `test` | Test addition/fix |
| `chore` | Build, deps, config |
| `adr` | Architecture decision |

**Scope**: `tracking`, `exposure`, `ui`, `db`, `api`, `map`, `service`, `test`

---

## Quick Reference: File Locations

```
road-guide/
├── ROAD GUIDE.txt                    # Original spec (read-only reference)
├── docs/
│   └── project-knowledge/            # AI Context Artifacts (THIS IS YOUR CONTEXT)
│       ├── 00_PROJECT_INDEX.md
│       ├── 01_PRODUCT_VISION.md
│       ├── 02_SYSTEM_ARCHITECTURE.md
│       ├── 03_DATA_MODEL_AND_DATABASE.md
│       ├── 04_GPS_AND_TRACKING.md
│       ├── 05_EXPOSURE_ENGINE_AND_FORMULAS.md
│       ├── 06_ENVIRONMENTAL_DATA_AND_INTEGRATIONS.md
│       ├── 07_ROUTING_AND_MAPS.md
│       ├── 08_UI_UX_AND_DESIGN_SYSTEM.md
│       ├── 09_API_AND_DATA_CONTRACTS.md
│       ├── 10_BACKGROUND_SERVICES_AND_PERMISSIONS.md
│       ├── 11_TESTING_AND_QUALITY.md
│       ├── 12_TECHNICAL_DECISIONS.md
│       ├── 13_CURRENT_IMPLEMENTATION_STATUS.md
│       └── 14_AI_WORK_RULES.md       # THIS FILE
├── design/
│   └── figma-export/                 # Exported Figma assets
│       ├── screens/
│       ├── components/
│       ├── icons/
│       ├── illustrations/
│       ├── logos/
│       ├── assets/
│       ├── styles/
│       └── FIGMA_EXPORT_INDEX.md
└── (source code - to be created)
```

---

## Enforcement

These rules are **not optional**. They exist because:
- Road Guide has precise formulas that must not drift
- Privacy is a core product promise
- The spec is 1800 lines — knowledge base prevents re-reading
- Greenfield project — early decisions compound

**If you violate a rule**: Stop, revert, re-read this file, do it correctly.

---

## Version

**1.0** — 2024-11-12 — Created with initial knowledge base.
Update version when rules change.