# GRA MASTER GDD — v0.4.0 Legacy Core + Visual Foundation

Status cap: 90% until Unity Editor compile + Play Mode smoke + Android device test all pass.

## Product truth
Production runtime: Unity 2022.3.62f2 + URP. Legacy APK defines gameplay/structure reference. Approved visual direction defines presentation: voxel/block 3D diorama, lush saturated greenery, layered cliffs, rivers/waterfalls, soft mobile-friendly light/depth.

The reference board is a collection of separate modules, never one overloaded screen. Primary routes: World, Buildings, Heroes, Research, Missions, Shop, Empire/Prestige. Achievements and Settings remain secondary routes.

## v0.4.0 playable contract
World -> encounter/stage -> player tap + combo -> enemy defeat -> currency -> building/hero progression -> passive DPS -> missions -> research modifiers -> boss/progression gate -> Empire/prestige reset -> persistent save/offline recovery -> return to World.

## Screen contracts
World owns the diorama and moment-to-moment interaction. Buildings owns production/upgrade decisions. Heroes owns roster and DPS growth. Research owns persistent modifiers/unlocks. Missions owns short objectives/claims. Shop owns monetization-facing catalog shell but v0.4.0 cannot require payment. Empire owns prestige preview/confirmation/reward/reset.

## Visual foundation
Gameplay readability wins over decoration. Cliffs form 2–4 readable height bands. Water must visually descend toward waterfall exits. Green terrain is dominant; stone/soil create edge contrast. Soft key/fill lighting and restrained bloom; no effects may hide tap targets or enemy state. Low-end path reduces particles, shadow distance and decorative animation without changing gameplay.

## Decision Log
DEC-029: GitHub Actions is the canonical build gate while local Unity availability is blocked.
DEC-030: Milestone remains 90% regardless of code/doc completeness until compile + Play Mode smoke + physical Android test evidence exists.
DEC-031: Reference board panels map to separate routes; no mega-screen implementation is accepted.
DEC-032: Visual work may advance behind stable gameplay interfaces, but cannot redefine legacy gameplay behavior.
DEC-033: v0.4.0 Shop is structurally present but monetization cannot block core progression.
DEC-034: CI must pin Unity 2022.3.62f2 and reject a reconstructed project with a different ProjectVersion.

## Dependency Map
Gameplay state -> World presenter -> Buildings/Heroes economy -> Research modifiers -> Missions observers -> Empire/prestige -> Save/offline serialization -> UI routing/presentation -> VFX/audio hooks -> analytics -> QA -> Release.

Parallel art lane: Art Bible -> Environment/Character/UI assets -> VFX -> optimization; integrates only through approved presenter interfaces.

## QA release gates
G1 Unity Editor compile: zero C# compile errors.
G2 Play Mode smoke: launch World, tap/kill, currency changes, navigate all primary routes, buy/upgrade, mission progress/claim, prestige preview/cancel, save/reload.
G3 Android build: CI produces installable artifact.
G4 Device smoke: cold launch, touch input, navigation, suspend/resume, save persistence, performance sanity on physical Android.
Only after G1–G4 may Centrala report >90% or DONE.

## Current risks
R1 Unity Personal CI license unavailable/invalid — blocks G1/G3. Mitigation: continue code/data/docs/assets; CI gate is ready.
R2 Reconstructed project archive may not contain all generated scene/meta dependencies — detected by pinned CI compile.
R3 Legacy parity drift during visual implementation — mitigate with presenter/state separation and legacy behavior acceptance tests.
R4 Mobile performance from voxel density/waterfalls/URP effects — enforce low-end variants and budgets before release.
R5 Save schema churn during integration — version every persisted schema and test migration/corruption recovery.
