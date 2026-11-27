# Progress Log (Milestones 1–7 Complete)
- **M1 Setup:** Kotlin + Jetpack Compose + Compose for TV (`tv-material`), `minSdk 28`, Fire TV manifest (`LEANBACK_LAUNCHER`, 320×180 banner, landscape, `FLAG_KEEP_SCREEN_ON`), no Play Services.
- **M2 Orbit Engine:** Pure-Kotlin JPL Keplerian + Meeus Moon `OrbitEngine` + `TimeController` + unit tests (`OrbitEngineTest`, `TimeControllerTest`, `FactCyclerTest`).
- **M3 Overview Scene:** 60 fps `drawWithCache` + `withFrameNanos`, twinkling starfield, evenly spaced orbit rings, glowing Sun, textured planets at real positions, Moon orbiting Earth, date + speed readout.
- **M4 Focus & Navigation:** D-pad Left/Right focus ring, OK to inspect, Up for Live reset, Down for High/Low quality toggle, Media Play/Pause, Fast Forward, Rewind.
- **M5 Detail View:** ~500 ms zoom transition, large lit planet on left, stats strip + 12 offline facts per body (`facts.json`) with cross-fade on OK, Left/Right switching, Back to overview.
- **M6 Lighting:** Shader-free rotated terminator shadow aligned to real Sun direction, Earth night-lights blend layer, Saturn rings + baked ring-shadow band.
- **M7 Polish:** Procedural placeholder textures bundled + `ASSETS.md` for 2K/4K NASA/Solar System Scope swap, `SIDELOAD.md`, High/Low quality mode, TV banner & icon.
