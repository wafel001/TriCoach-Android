# GRA MASTER GDD — v0.4.0 Legacy Core + Visual Foundation

Status cap: 90% until Unity Editor compile + Play Mode smoke + Android device test all pass.

## Product truth
Production runtime: Unity 2022.3.62f2 + URP. Legacy APK/source defines gameplay/structure reference. Approved visual direction defines presentation: voxel/block 3D diorama, lush saturated greenery, layered cliffs, rivers/waterfalls, soft mobile-friendly light/depth.

Gameplay source is frozen during the visual recovery lane. CI hashes `Assets/Scripts/Gameplay/*.cs` before and after every visual patch and rejects any drift.

The reference board is a collection of separate modules, never one overloaded screen. Primary routes: World, Buildings, Heroes, Research, Missions, Shop, Empire/Prestige. Achievements and Settings remain secondary routes.

## v0.4.0 playable contract
World -> encounter/stage -> player tap + combo -> enemy defeat -> currency -> building/hero progression -> passive DPS -> missions -> research modifiers -> boss/progression gate -> Empire/prestige reset -> persistent save/offline recovery -> return to World.

## Screen contracts
World owns the diorama and moment-to-moment interaction. Buildings owns production/upgrade decisions. Heroes owns roster and DPS growth. Research owns persistent modifiers/unlocks. Missions owns short objectives/claims. Shop owns monetization-facing catalog shell but v0.4.0 cannot require payment. Empire owns prestige preview/confirmation/reward/reset.

## Visual foundation
Gameplay readability wins over decoration. Cliffs form 2–4 readable height bands. Water must visually descend toward waterfall exits. Green terrain is dominant; stone/soil create edge contrast. Soft key/fill lighting and restrained bloom; no effects may hide tap targets or enemy state. Low-end path reduces particles, shadow distance and decorative animation without changing gameplay.

Rendering contract: production Android path is URP only. Runtime voxel materials use `GRA/VoxelColor`, a project-owned URP shader included through Resources and compiled in the Unity gate. Built-in `Standard`, runtime-only `Shader.Find` fallbacks and pipeline switching are not accepted production fixes.

## Decision Log
DEC-029: GitHub Actions is the canonical reproducible build gate while local Unity availability is not guaranteed.
DEC-030: Milestone remains 90% regardless of code/doc completeness until compile + Play Mode smoke + physical Android test evidence exists.
DEC-031: Reference board panels map to separate routes; no mega-screen implementation is accepted.
DEC-032: Visual work may advance behind stable gameplay interfaces, but cannot redefine legacy gameplay behavior.
DEC-033: v0.4.0 Shop is structurally present but monetization cannot block core progression.
DEC-034: CI must pin Unity 2022.3.62f2 and reject a reconstructed project with a different ProjectVersion.
DEC-035: A successful APK upload is not a runtime pass. Physical device evidence overrides CI success for visual/UI acceptance.
DEC-036: Android compatibility is verified from the compiled APK manifest with `aapt`; ProjectSettings alone are not accepted as evidence.
DEC-037: Gameplay source hash must be identical before/after visual rebuilding. Visual/UI/Editor layers may change; gameplay source may not.
DEC-038: Production renderer is restored to URP. The temporary Built-in v0.5.0 build is retained only as a diagnostic artifact and is not the visual foundation.
DEC-039: Runtime voxel shader is project-owned and URP-tagged; CI fails if URP package, active URP asset or shader compile support is missing.

## Dependency Map
Frozen legacy Gameplay -> World presenter -> URP VisualMaterialLibrary/GRA-VoxelColor -> Buildings/Heroes economy -> Research modifiers -> Missions observers -> Empire/prestige -> Save/offline serialization -> separate UI routes -> VFX/audio hooks -> analytics -> QA -> Release.

Android release path: Unity 2022.3.62f2 -> URP compile gate -> IL2CPP ARM64 build -> APK manifest inspection -> Play Mode smoke evidence -> physical Android smoke -> release candidate.

Parallel art lane: Art Bible -> Environment/Character/UI assets -> VFX -> optimization; integrates only through approved presenter interfaces.

## QA release gates
G1 Unity Editor compile: zero C# compile errors and URP shader compiles without ShaderUtil errors.
G2 Play Mode smoke: launch World, tap/kill, currency changes, navigate all primary routes, buy/upgrade, mission progress/claim, prestige preview/cancel, save/reload.
G3 Android build: CI produces installable ARM64 artifact and compiled APK reports targetSdkVersion >= 35.
G4 Device smoke: cold launch, no magenta materials, HUD visible, touch input, navigation, suspend/resume, save persistence, performance sanity on physical Android.
Only after G1–G4 may Centrala report >90% or DONE.

## Current verified state
- Diagnostic v0.5.0: Unity Android build PASS; APK versionName 0.5.0/versionCode 50; minSdk 26; compiled targetSdkVersion 36; ARM64. This proves the previous Android compatibility warning is no longer explained by target SDK in the newest artifact.
- v0.5.0 is Built-in renderer diagnostic only and does not satisfy the approved URP product contract.
- v0.5.1: URP recovery lane started. It preserves gameplay hashes, uses a Resources-backed URP shader and hard-fails if the reconstructed project has no active `UniversalRenderPipelineAsset`.
- Play Mode and physical-device acceptance remain open; milestone stays at 90%.

## Current risks
R1 URP asset reference in reconstructed ProjectSettings may be absent/stale. Mitigation: v0.5.1 hard-gates `GraphicsSettings.defaultRenderPipeline` as `UniversalRenderPipelineAsset`; repair the asset reference rather than silently switching pipelines.
R2 Magenta rendering can still occur on device from shader/API incompatibility even after Editor shader compile. Mitigation: custom URP shader, Resources inclusion, Vulkan/GLES3 device smoke.
R3 HUD initialization remains physically unverified. Mitigation: deterministic RuntimeFontProvider and device acceptance requiring visible HUD.
R4 Legacy parity drift during visual implementation. Mitigation: gameplay SHA diff + behavior stress tests; no gameplay edits in visual lane.
R5 Mobile performance from voxel density/waterfalls/URP effects. Mitigation: low-end variants and budgets before release.
R6 Save/schema regressions are not covered by visual stress simulation alone. Mitigation: Play Mode smoke includes save/reload and later corruption/migration tests.
