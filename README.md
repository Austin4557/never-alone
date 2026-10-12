# Never Alone Forge 1.20.1 Stage 8 — Worn Armor UV Fix

Compiled through GitHub Actions workflow. This stage replaces the old Fabric 26.2 equipment atlases with six UV-complete Forge 1.20.1 worn-armor maps. See `STAGE8_ARMOR_TEXTURE_FIX.md`.

# Never Alone - Forge 1.20.1 Stage 6

**Source only.** Not a compiled or game-tested Forge JAR.
Target: Minecraft **1.20.1**, Forge **47.4.10**, Java **17**.

## What's new over Stage 5

- Added `/na check`, `/na check armor`, `/na check eggs`, and `/na check loot`.
  These are registry/NBT diagnostics that will work only after a real compiled Forge mod loads.
- Added `/na director interval <45..300>` and an expanded `/na director status`.
  Minor ambient effects remain opt-in and reset on disconnect.
- Fixed director scheduling to rely on global server ticks across world dimensions.
- The original armor registration, crafting, eight scare tests and named egg commands remain.
- The additive 20% Nether Fortress loot modifier is preserved. `/na check loot` checks
  codec registration only; it is **not** proof that a chest reward is generated.

## Compiling

This environment cannot reach Forge Maven, and **no runnable JAR** is included.
Use the supplied `BUILD_WITH_FORGE_MDK.bat` with the Forge 47.4.10 MDK or run the
`.github/workflows/forge-build.yml` workflow in your own GitHub repository.
Both require Java 17 and network access to ForgeGradle dependencies.

Before building, run `python verify_source.py`. If ForgeGradle succeeds, run
`python verify_jar.py build/libs`. These are static checks, not substitutes for
an in-game launch test. Do not put the source ZIP in the `mods` folder.

## First game test after a verified build

1. Use a **copy** of the Forge instance and back up your world.
2. Ensure the old broken Never Alone experimental JARs/datapacks are disabled.
3. With v7 cat resource pack active, run `/na status`, then `/na check`.
4. Test `/na armor equip rhinestone`, `/na armor equipped`, `/na eggs`.
5. Opt into `/na director on` then `/na director interval 90` to test quiet cues.
6. Open fresh Nether Fortress loot only after initial startup is stable.

## Not ported

Full Fabric stalker, advanced horror controller, high-detail armor geometry and
actual rendered armor look; no testing against Embeddium/Oculus yet.

See `STAGE6_CHANGES.txt` and earlier changelog files for details.
