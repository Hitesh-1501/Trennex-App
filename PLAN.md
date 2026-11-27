# TrenNex plan

TrenNex is a production-grade **portfolio and resume project**, not currently a live commercial store. Its purpose is to demonstrate Android, Firebase backend, AI, data, security, testing, and DevOps engineering as one coherent system.

## Source of truth

- `TRENNEX_FEATURE_ROADMAP.md` — all 17 owner requirements plus advanced features, dependencies, staging acceptance criteria, and implementation order.
- `DEVELOPMENT_STATUS.md` — what has actually been built and verified, what remains, and which Firebase project was touched.
- `FIGMA_SCREEN_INVENTORY.md`, `FIGMA_ROUTE_MAPPING.md`, and `FIGMA_IMPLEMENTATION_PLAN.md` — Figma frame coverage, route decisions, and screen-level visual workflow.

## Current focus

Roadmap **Milestone A: stability** has its core implementation complete, with acceptance verification pending. On 2026-11-25 the owner explicitly approved a sequencing exception: proceed with **Milestone B: Engineering Foundation** before closing A's remaining checks. B1 GitHub Actions CI/CD, B2 Firebase Analytics, and B3 Firebase Crashlytics are complete by owner acceptance. The owner manually verified staging fatal and non-fatal Crashlytics reports and approved B4 security hardening on 2026-11-27. Keep individually unverified B2/B3 privacy, consent, environment isolation, and regression checks in the combined A+B acceptance list. B4 remains in verification until the owner reviews the local emulator evidence and approves a staging deployment; B5 and B6 have not started. Preserve the documented B3 shared application ID isolation risk and exported staging test activity limitation. After B, run combined A+B staging regression tests and request the owner's approval before starting C. Keep every unfinished A or B check in `DEVELOPMENT_STATUS.md`; this exception does not waive acceptance.

## Environment gate

All app code, Firebase data, Cloud Functions, web admin work, integrations, and testing target local emulators or `trennex-staging-ebbeb` first. Payment, shipping, and seeded review demonstrations must be visibly identified as demo/test data. Do not modify or deploy to `trennex-ebbeb` without the owner's explicit approval after staging acceptance and a production migration/release review.
