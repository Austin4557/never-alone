# Never Alone Forge 1.20.1 — v9 visual polish / Stage 10 source

Built from the last working Stage 9 code baseline. All eleven Forge Java classes are unchanged.
Adds updated worn armor textures plus valid pack.mcmeta; version metadata changed for clear testing.

The cats remain a **standalone** ETF/EMF resource pack, since the current mod uses named vanilla cats.
Install the compiled Stage 10 JAR in mods, and enable v9 CATS_AND_ARMOR pack above mod resources.
The combined pack can supply armor as a temporary override for the older Stage 9 JAR; this source builds the same armor art into the mod JAR.

Minecraft 1.20.1 / Forge 47.4.10 / Java 17. No new gameplay systems in this visual-only pass.
The full stalker port is still unfinished.

Build with ForgeGradle 6 and Gradle 8.8 on Java 17; see the project's existing build instructions.
A successful Gradle build does NOT mean in-game rendering and animation have been verified.
