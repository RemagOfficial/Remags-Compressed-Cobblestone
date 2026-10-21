# Session Handoff — Dynamic Single-Block Compressed Cobblestone

## The architecture in one line
Instead of ~10k blocks/items/blockstates/models/recipes (one per level), the whole mod is now
**ONE block + ONE item**. The compression level is *data*, not a registry entry:

- **Block** `rcc:compressed_cobblestone` — an `EntityBlock`; the level lives in a synced
  `CompressionBlockEntity`.
- **Item** `rcc:compressed_cobblestone` — a `BlockItem` subclass; the level lives on a
  **data component** `rcc:compression_level` (a plain `Codec.INT`).
- **Runtime tint** — `BlockColor`/`ItemColor` handlers read the BE/component and apply
  `CompressionConfig.tintArgb(level)`. No baked per-level colours, no per-level PNGs, no per-level models.
- **Per-level names** — `CompressedCobblestoneItem.getName(stack)` returns
  `DisplayNames.compressed(level)` at runtime. One lang key for the base name, not 10k.

The single source of truth for the level is the `rcc:compression_level` component; the block entity,
the dropped item, and pick-block all round-trip through it.

## The one knob (unchanged meaning, new blast radius)
`src/main/java/com/remag/rcc/registry/CompressionConfig.java`
```java
public static final int MAX_LEVELS = 10000;
```
`MAX_LEVELS` is now purely a **runtime cap** — it no longer drives datagen or registry size. It gates:
- how many entries the creative tab shows (`for level 1..MAX_LEVELS`, each a component-carrying stack);
- the tint ramp (`brightness(level) = 1 - level / MAX_LEVELS`, so level 1 barely darkens and the top level goes black);
- the upper bound of the compress recipe (refuses when `level+1 > MAX_LEVELS`).

Changing it needs **no** `runData` — it's read at runtime.

## File map (all under `src/main/java/com/remag/rcc/`)
### Core / registration
- `RCC.java` — mod constructor. Registration order on the mod bus: `ModDataComponents` →
  `ModBlocks` → `ModBlockEntities` → `ModItems` → `ModRecipeSerializers` → `ModCreativeModeTab.TABS`.
  Client tint handlers are wired only under `if (FMLEnvironment.dist == Dist.CLIENT)` so the
  client-only class is never loaded on a dedicated server.
- `registry/ModDataComponents.java` — `DeferredRegister<DataComponentType<?>>` on
  `Registries.DATA_COMPONENT_TYPE`; `COMPRESSION_LEVEL` over `Codec.INT`. Always read via
  `stack.getOrDefault(COMPRESSION_LEVEL, 1)`.
- `block/ModBlocks.java` — single `DeferredBlock<Block> COMPRESSED_COBBLESTONE`.
- `block/CompressedCobblestoneBlock.java` — `Block implements EntityBlock`. `newBlockEntity`,
  `setPlacedBy` copies the placing stack's component into the BE + syncs, `getCloneItemStack`
  (pick-block) carries the BE level. Keeps cobblestone properties with `.strength(2f).requiresCorrectToolForDrops()`.
- `block/entity/CompressionBlockEntity.java` — mutable `int compressionLevel`; persists via
  `saveAdditional`/`loadAdditional`, syncs to client via update tag, and overrides
  `collectImplicitComponents(...)` to publish `COMPRESSION_LEVEL` (1.21.1 has no
  `CopyableComponentsHolder`; `minecraft:copy_components` from `block_entity` reads this). `setLevel`
  calls `setChanged()` + `level.sendBlockUpdated(...)`.
- `block/entity/ModBlockEntities.java` — `DeferredRegister<BlockEntityType<?>>` bound to the block.
- `item/ModItems.java` — single `DeferredItem<Item> COMPRESSED_COBBLESTONE`.
- `item/CompressedCobblestoneItem.java` — `BlockItem`; `getName(stack)` → `DisplayNames.compressed(level)`.
- `block/ModCreativeModeTab.java` — `displayItems` loops `1..MAX_LEVELS`, emitting a component-carrying
  stack per level; icon is the level-1 stack.

### Recipes (1 static + 2 dynamic)
- `recipe/CompressRecipe.java` — `CraftingRecipe` (i.e. `Recipe<CraftingInput>` *and* the crafting marker
  interface). `matches` requires the 3×3 to be nine of the
  same compressed item at one level `n` (exactly one per slot); assembles `n+1`; refuses when `n+1 > MAX_LEVELS`.
- `recipe/DecompressRecipe.java` — matches a single compressed stack at level `n` (one item, all other slots
  empty); assembles 9× of `n-1`, or 9× vanilla cobblestone when `n == 1`.
- `recipe/CompressRecipeSerializer.java` / `DecompressRecipeSerializer.java` — **stateless** serializers
  (`MapCodec.unit` / `StreamCodec.unit`), so the recipe JSON is just `{"type":"rcc:compress"}`.
- `recipe/ModRecipeSerializers.java` — `DeferredRegister` on `Registries.RECIPE_SERIALIZER`. Both recipes
  use the vanilla `RecipeType.CRAFTING` type (a custom `RecipeType` is not matched by `CraftingMenu`), and therefore
  **must implement `CraftingRecipe`, not just `Recipe<CraftingInput>`**: consumers like JEI cast every
  crafting-type recipe to `CraftingRecipe`, so a bare `Recipe` throws `ClassCastException` on world join.
  Each implements the required `category()` (returns `CraftingBookCategory.MISC`).

### Client runtime tint
- `event/ClientEvents.java` — explicit `register(IEventBus)` (not the deprecated `@EventBusSubscriber`)
  adding `RegisterColorHandlersEvent.Block` (`BlockColor` reads the BE level at the position) and
  `RegisterColorHandlersEvent.Item` (`ItemColor` reads the stack's `COMPRESSION_LEVEL`), both mapping through
  `CompressionConfig.tintArgb(level)`.

### JEI
- `jei/RccJEIPlugin.java` — `@JeiPlugin`. The dynamic recipes carry no concrete tier, so vanilla JEI can't
  draw them. The plugin registers one `CompressionCategory` and feeds it a handful of illustrative
  example tiers (compress 1→2→3, decompress 3→2→1→vanilla); the mechanic generalises to every level.
- `jei/CompressionCategory.java` — lays a full 3×3 grid for compression, a single centred input for
  decompression, plus the output. Example stacks carry the component so they render through the same
  item tint. (Avoids the deprecated `getBackground()`; no fixed-width background drawable.)
- `jei/CompressionDisplayRecipe.java` — display-only record `(List<ItemStack> inputs, ItemStack output)`.

### Datagen (now collapses to one set)
- `datagen/ModBlockStateProvider.java` — one plain-cube block model (`parent minecraft:block/block`,
  texture `cobble` = `minecraft:block/cobblestone`, **no baked colour** so runtime tint applies) +
  `simpleBlock` + `simpleBlockItem`. No loop.
- `datagen/loot/ModBlockLootTables.java` — `createSingleItemTable(block.asItem())` with
  `.apply(CopyComponentsFunction.copyComponents(Source.BLOCK_ENTITY))` so drops keep the level.
- `datagen/ModRecipeProvider.java` — writes the static `9× cobblestone → compressed(L1)` shaped recipe
  (result carries `rcc:compression_level: 1`) plus one instance each of the two dynamic recipes.
- `datagen/ModLanguageProvider.java` — emits only the base block name + the creative-tab name.
- `datagen/ModBlockTagGenerator.java` — adds the single block to `MINEABLE_WITH_PICKAXE`.

### Retired
- `registry/NumberSpeller.java` — deleted (IDs are no longer per level).
- The old per-level loop in `ModBlocks`/`ModItems` and the 105 legacy block textures under
  `src/main/resources/assets/rcc/textures/block/` are gone; models reference the vanilla texture.

## Verification done (headless)
- `gradlew compileJava` — BUILD SUCCESSFUL, no deprecation warnings (the JEI `getBackground()` warning is
  resolved).
- `gradlew runData` — BUILD SUCCESSFUL. Generated output collapsed to exactly:
  - 1 blockstate (`blockstates/compressed_cobblestone.json`)
  - 1 block model + 1 item model (both plain, no baked colour)
  - 3 recipes: `compressed_cobblestone_one.json` (static, ingredient `minecraft:cobblestone`, result
    component `rcc:compression_level: 1`), `compression.json` (`{"type":"rcc:compress"}`),
    `decompression.json` (`{"type":"rcc:decompress"}`)
  - 1 loot table (`functions: copy_components, source: block_entity`)
  - `en_us.json` with 2 entries (`block.rcc.compressed_cobblestone`, `itemGroup.rcctab`)
- `gradlew build` — BUILD SUCCESSFUL (jar assembles; JEI dep is `compileOnly`, so it isn't shipped).

## Not verified in-game (needs your eyes)
`runClient` was intentionally never run here (headless-only workflow). Please confirm in a client launch:
- GUI/held/placed icons show the correct per-level shade and darken smoothly across the ramp.
- The single plain-cube model actually tints (block colour multiplies every quad).
- Breaking a placed block drops an item that keeps its level; pick-block (middle-click) keeps its level.
- Placing a component-carrying item yields a BE with the matching level.
- 3×3 of tier N → tier N+1; single tier N → 9× tier N-1; tier 1 → 9× vanilla cobblestone.
- Compress refuses above `MAX_LEVELS`.
- JEI shows the `rcc:compression` category examples (requires JEI present at runtime — it's `compileOnly`).

## Gradle note
`build.gradle` now has (JEI is `compileOnly` so the plugin compiles but isn't bundled):
```gradle
localRuntime    "mezz.jei:jei-${minecraft_version}-neoforge:19.69.0.456"
compileOnly     "mezz.jei:jei-${minecraft_version}-common-api:19.69.0.456"
```

## To add/remove levels later
Just edit `CompressionConfig.MAX_LEVELS`. No datagen, no registry change — creative-tab entries, tint ramp,
and the compress upper bound all follow the constant at runtime.
