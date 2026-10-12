# Never Alone Forge 1.20.1 Stage 8 — worn armor texture layout fix

The prior Stage 7 JAR already registered, equipped and compiled correctly, but copied
Minecraft Fabric 26.2 equipment PNGs into Minecraft Forge 1.20.1 worn armor paths.
These PNGs were much too sparse for the old armor UV unwrap; in particular,
leggings had only a small occupied region.

This pass rebuilds exactly six worn armor PNGs (three sets x layers 1 and 2)
on an old-style 64 x 32 armor UV atlas, at 4x native resolution (256x128).
The vanilla biped helmet, torso, arm, boot and leg surfaces have been populated,
keeping Rhinestone in pink (heart centered on torso, narrow tiara band),
Emerald Warden green/gold, and Dark Amethyst purple/silver.

- Item IDs, registration, armor stats, crafting recipes, the named cats,
  commands, scare tests and Fortress loot remain unchanged.
- V8 cat resource pack is still separate and should remain enabled.
- No custom armor geometry or shader-driven glint is claimed yet.
- A real ForgeGradle build is required; texture inspection alone is NOT
  a guarantee of rendering in a modpack. Test with Embeddium and Oculus.

Remove the Stage 7 JAR before placing the new Stage 8 JAR in mods. Never
install both, since they have the same mod ID (`never_alone`).

In game (operator permission):
    /na check armor
    /na armor equip rhinestone
    /na armor equip emerald_warden
    /na armor equip dark_amethyst

If the armor still disappears, send a screenshot and the latest.log;
the remaining suspect would be third-party render interception rather than
missing atlas coverage.
