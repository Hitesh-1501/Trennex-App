# TrenNex plan

TrenNex is a production-grade **portfolio and resume project**, not currently a live commercial store. Its purpose is to demonstrate Android, Firebase backend, AI, data, security, testing, and DevOps engineering as one coherent system.

## Source of truth

- `TRENNEX_FEATURE_ROADMAP.md` — all 17 owner requirements plus advanced features, dependencies, staging acceptance criteria, and implementation order.
- `DEVELOPMENT_STATUS.md` — what has actually been built and verified, what remains, and which Firebase project was touched.
- `FIGMA_SCREEN_INVENTORY.md`, `FIGMA_ROUTE_MAPPING.md`, and `FIGMA_IMPLEMENTATION_PLAN.md` — Figma frame coverage, route decisions, and screen-level visual workflow.

## Current focus

Roadmap **Milestone A: stability** is in progress. The first add-address and rapid-cart guards are implemented in staging; shared connectivity, loading, error, empty, retry, and lifecycle handling is under verification. Do not start Milestone B until Milestone A passes its staging checks and the owner approves it.

## Environment gate

All app code, Firebase data, Cloud Functions, web admin work, integrations, and testing target local emulators or `trennex-staging-ebbeb` first. Payment, shipping, and seeded review demonstrations must be visibly identified as demo/test data. Do not modify or deploy to `trennex-ebbeb` without the owner's explicit approval after staging acceptance and a production migration/release review.
