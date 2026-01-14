# OAR-DDS Routing Problem and Possible Solution

## The Problem: URL Structure

### Monolith Endpoint Structure

The monolith uses `context-path: /od` with controllers at `/ds`:

| Endpoint | Full Path | What It Does |
|----------|-----------|--------------|
| Dataset files | `/od/ds/{dsid}/{filepath}` | Download dataset files |
| AIP bags | `/od/ds/_aip/{aipid}` | Access archive packages |
| RPA requests | `/od/ds/rpa/request` | Restricted data requests |
| Bundle plan | `/od/ds/_bundle_plan` | Create download plan |
| Bundle download | `/od/ds/_bundle` | Stream zip bundle |
| Cache mgmt | `/od/cache/**` | Internal cache operations |

**The `/od/ds/` prefix is baked into:**
- All external client applications (SDP, RPA apps)
- Documentation and user guides
- Bookmarks and saved URLs
- Scripts and automation tools

---

## The Challenge: Breaking Up the Monolith

### When we split into microservices, what happens to URLs?

**Monolith (1 app, 1 context):**
```
oar-dist-service
  └── context-path: /od
        └── /ds/** → all distribution logic
        └── /cache/** → cache logic
```

**Microservices (6 apps, 6 contexts):**
```
dataset-access-service    → needs /ds/{dsid}/**
aip-access-service        → needs /ds/_aip/**
restricted-access-service → needs /ds/rpa/**
bundle-plan-service       → needs /ds/_bundle_plan
data-bundle-service       → needs /ds/_bundle
cache-mgmt-service        → needs /cache/**
```

**Problem:** 6 separate apps cannot all own `/od/ds/` simultaneously

---

## Two Options

### Option A: Change Client URLs (Breaking Change)

Give each microservice its own unique path:

| Service | New Path | Old Path |
|---------|----------|----------|
| dataset-access | `/dataset/**` | `/od/ds/{dsid}/**` |
| aip-access | `/aip/**` | `/od/ds/_aip/**` |
| restricted-access | `/rpa/**` | `/od/ds/rpa/**` |
| bundle-plan | `/bundle/plan/**` | `/od/ds/_bundle_plan` |
| cache-mgmt | `/cache/**` | `/od/cache/**` |

**Impact:**
- All clients must update their code
- Documentation must be rewritten
- Saved URLs break

---

### Option B: API Gateway Handles Translation (No Breaking Change)

Keep external URLs unchanged, gateway routes internally:

```
Client Request          Gateway Translation      Service
─────────────────────────────────────────────────────────
/od/ds/mds2-2106/...  → rewrite to /ds/...    → dataset-access
/od/ds/_aip/...       → rewrite to /ds/_aip/  → aip-access
/od/ds/rpa/...        → rewrite to /ds/rpa/   → restricted-access
/od/ds/_bundle_plan   → route to /ds/         → bundle-plan
/od/cache/...         → route to /cache/      → cache-mgmt
```

**Impact:**
- Zero client changes
- URLs work exactly as before
- Gateway absorbs all complexity

---

## How the Gateway Solves This

```
External Clients (SDP, RPA apps, browsers, scripts)
  - Still use: /od/ds/mds2-2106/trial1.json
  - No changes required

↓

API Gateway receives the request and:
  1. Matches route: /od/ds/** → dataset-access-service
  2. Rewrites path: /od/ds/... → /ds/...
  3. Forwards to: lb://dataset-access-service/ds/...

↓

dataset-access-service (port 8081)
  - Receives: /ds/mds2-2106/trial1.json
  - Controller: @RequestMapping("/ds")
  - Returns file to client


```

---

## Why Option B is Better

| Criteria | Option A (New URLs) | Option B (Gateway) |
|----------|--------------------|--------------------|
| Client code changes | Required for all clients | None |
| Rollback capability | Complex | Instant (nginx switch) |
| Timeline | Long | Short |
| Documentation | Full rewrite | No changes |
| Backward compatibility | Broken | Preserved |

**Gateway absorbs the `/od/ds/` complexity so clients don't have to change.**

- External URLs stay the same forever
- Internal routing can evolve independently
- Future API versions can coexist (v1, v2)
- Single place for all routing logic

---

## Summary

| Layer | What It Sees | Responsibility |
|-------|--------------|----------------|
| **Clients** | `/od/ds/**`, `/od/cache/**` | Unchanged - no migration needed |
| **nginx** | Routes to gateway or monolith | Coexistence & rollback switch |
| **Gateway** | Translates `/od/ds/**` → services | Path rewriting, load balancing |
| **Services** | Clean paths (`/ds/`, `/cache/`) | Business logic only |

**Result:** Microservices architecture with zero breaking changes to external consumers.
