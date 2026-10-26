package com.remag.rcc.datagen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.remag.rcc.RCC;
import com.remag.rcc.block.ModBlocks;
import com.remag.rcc.registry.CompressionBand;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * Emits the whole {@code rcc_manual} Patchouli book as JSON, so the in-world multiblock previews are <b>generated
 * from the {@link CompressionBand} table</b> rather than hand-typed - the book and the structure scanners can never
 * drift apart (see {@code MASTER_PLAN.md} §5 / D26).
 *
 * <p>Patchouli content is pure data, so this provider touches no {@code vazkii.patchouli.*} class: it builds
 * {@link JsonObject}s and writes them with {@link DataProvider#saveStable}. It honours Patchouli's split layout for a
 * {@code use_resource_pack} book - the book definition and the guide-book recipe land under {@code data/<ns>/...},
 * while the localised {@code categories/} and {@code entries/} land under {@code assets/<ns>/...}.
 *
 * <p>Only bands 0-1 are written up here because that is all that is shipped; the multiblock geometry is read from
 * {@link CompressionBand#BAND_1} so a later band reuses {@link #multiblockPage} by adding rows to the entry list, not
 * by re-deriving a pattern.
 */
public class ModPatchouliBookProvider implements DataProvider {
    /** The book's path component; the full resource id is {@code rcc:rcc_manual}. */
    private static final String BOOK = "rcc_manual";
    private static final String LANG = "en_us";
    /**
     * How much <b>rendered</b> text (formatting macros stripped) one full {@code patchouli:text} page shows before
     * {@code resize} would shrink it. Packing against this rather than the raw macro-inflated string is what stops
     * macro-heavy sentences from spilling a stub onto a fresh page while the previous one still had room.
     */
    private static final int MAX_PAGE_CHARS = 220;
    /** Matches a Patchouli formatting macro such as {@code $(l)}, {@code $()}, {@code $(#00AA00)} - zero rendered width. */
    private static final Pattern MACRO = Pattern.compile("\\$\\([^)]*\\)");

    private final PackOutput output;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /**
     * One band's manual write-up. Only the <em>editorial</em> content lives here - the geometry of every multiblock
     * preview is still derived live from {@link #band()} through {@link #multiblockObject}, so the book cannot drift
     * from the scanner. Adding a band is one entry in {@link #BANDS} plus its prose, never a new render path.
     */
    private record BandWriteup(
            CompressionBand band,
            String folder,           // the category key and the entry folder (band1, band2, ...)
            String categoryName,
            String categoryDesc,
            String compressorItem,   // item + recipe id of the band's compressor core
            String decompressorItem,
            String generatorItem,
            String coreLabelSuffix,  // appended to the three craft-page titles ("", " II", ...)
            String compDoes,
            String compShellTitle,
            String decompDoes,
            String decompShellTitle,
            String genDoes,
            String genTowerTitle) {
    }

    /**
     * The structure bands the manual documents, oldest first. Each row renders through {@link #manipulatorEntries}
     * along one shared code path; band 1's strings are the shipped manual verbatim, so its generated JSON is unchanged.
     */
    private static final List<BandWriteup> BANDS = List.of(
            new BandWriteup(CompressionBand.BAND_1, "band1",
                    "Tier 1 - The Manipulators",
                    "The three multiblocks, built from one shared shell.",
                    "compression_core", "compression_decompressor_core", "generator_core", "",
                    "It takes a seed of at least $(l)tier 1000$() and $(b)doubles its tier$() - a tier $(#00AA00)n$() block"
                            + " becomes a tier $(#00AA00)2n$() block - climbing from there to the band's ceiling of"
                            + " $(l)10^15$().",
                    "5x5x5 Shell",
                    "The band-1 decompressor - the multiblock-scale version of the Auto Decompressor. Feed it a goal tier and"
                            + " it steps a giant block back down one tier at a time, releasing a torrent of BCE as it goes.",
                    "3x3x3 Shell",
                    "The tier-1 generator tower seals a Generator Core in one of the two slots of a four-tall, 3-wide"
                            + " column. It mints BCE on the $9^n$ curve, from a $(l)tier-1000$() block's worth per second up"
                            + " to half a tier-$(l)10^15$() block.",
                    "3x3x4 Tower"),
            new BandWriteup(CompressionBand.BAND_2, "band2",
                    "Tier 2 - The Wider Manipulators",
                    "The same three machines, scaled up off the band table.",
                    "compression_core_2", "compression_decompressor_core_2", "generator_core_2", " II",
                    "It takes a seed of at least $(l)tier 10^15$() and $(b)doubles its tier$() - a tier $(#00AA00)n$()"
                            + " block becomes a tier $(#00AA00)2n$() block - climbing from there to the band's ceiling of"
                            + " $(l)10^27$().",
                    "7x7x7 Shell",
                    "The band-2 decompressor - the multiblock-scale version of the Auto Decompressor. Feed it a goal tier and"
                            + " it steps a giant block back down one tier at a time, releasing a torrent of BCE as it goes.",
                    "5x5x5 Shell",
                    "The tier-2 generator tower seals a Generator Core in one of the two slots of a six-tall, 5-wide"
                            + " column. It mints BCE on the $9^n$ curve, from a $(l)tier-10^15$() block's worth per second"
                            + " up to half a tier-$(l)10^27$() block.",
                    "5x5x6 Tower"));

    public ModPatchouliBookProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public String getName() {
        return "RCC Patchouli Manual";
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Path dataRoot = this.output.getOutputFolder().resolve("data").resolve(RCC.MODID);
        Path assetsRoot = this.output.getOutputFolder().resolve("assets").resolve(RCC.MODID);
        Path bookData = dataRoot.resolve("patchouli_books").resolve(BOOK);
        Path bookAssets = assetsRoot.resolve("patchouli_books").resolve(BOOK).resolve(LANG);

        List<CompletableFuture<?>> futures = new ArrayList<>();

        // Book definition (data tree) + the craft that hands it out. The recipe must land under {@code recipe/}
        // (singular) - Minecraft 1.21 renamed the recipe datapack folder - or it is never loaded.
        futures.add(save(cache, bookData.resolve("book.json"), bookJson()));
        futures.add(save(cache, dataRoot.resolve("recipe").resolve(BOOK + ".json"), bookRecipe()));

        // Chapters.
        futures.add(save(cache, bookAssets.resolve("categories").resolve("intro.json"),
                category("The Cobblestone Way", "How this book works and what BCE is.", item("compressed_cobblestone"))));
        futures.add(save(cache, bookAssets.resolve("categories").resolve("band0.json"),
                category("Tier 0 - The Single Blocks", "The handcrafted climb and the first automation.", item("compression_auto"))));
        // One chapter per structure band, each named for and iconed by its own compressor core.
        for (BandWriteup w : BANDS) {
            futures.add(save(cache, bookAssets.resolve("categories").resolve(w.folder() + ".json"),
                    category(w.categoryName(), w.categoryDesc(), item(w.compressorItem()))));
        }
        futures.add(save(cache, bookAssets.resolve("categories").resolve("logistics.json"),
                category("BCE Logistics", "Moving and storing raw BCE between machines.", item("bce_pipe"))));

        // Entries.
        for (var entry : introEntries()) {
            futures.add(save(cache, bookAssets.resolve("entries").resolve(entry.folder()).resolve(entry.file()), entry.json()));
        }
        for (var entry : band0Entries()) {
            futures.add(save(cache, bookAssets.resolve("entries").resolve(entry.folder()).resolve(entry.file()), entry.json()));
        }
        for (BandWriteup w : BANDS) {
            for (var entry : manipulatorEntries(w)) {
                futures.add(save(cache, bookAssets.resolve("entries").resolve(entry.folder()).resolve(entry.file()), entry.json()));
            }
        }
        for (var entry : logisticsEntries()) {
            futures.add(save(cache, bookAssets.resolve("entries").resolve(entry.folder()).resolve(entry.file()), entry.json()));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    // --- Book scaffolding ----------------------------------------------------------------------

    private JsonObject bookJson() {
        JsonObject book = new JsonObject();
        book.addProperty("name", "Remag's Compressed Cobblestone");
        book.addProperty("author", "Remag");
        // Keep the landing text short: the cover page only has a small box, and Patchouli's overflow handling shrinks
        // the font to fit a long string, which clips glyphs into unreadable text (the reported "first page too small").
        book.addProperty("landing_text",
                "Everything here starts as cobblestone and ends as a number too big to write down. Nine blocks of a tier"
                        + " craft into one of the next; a block's mass is its 9^n in Base Cobblestone Equivalents (BCE).");
        book.addProperty("text_overflow_mode", "resize");
        book.addProperty("version", 1);
        book.addProperty("use_resource_pack", true);
        book.addProperty("creative_tab", RCC.MODID + ":rcc_machines_tab");
        book.addProperty("show_progress", false);
        book.addProperty("read_only", false);
        return book;
    }

    /**
     * A plain {@code minecraft:crafting_shapeless} that yields the guide book, so it shows up in JEI and the recipe
     * book. We deliberately avoid Patchouli's own {@code patchouli:shapeless_book_recipe}: that is a <em>custom</em>
     * recipe type JEI does not index in its vanilla-crafting category, so the book would look uncraftable. The vanilla
     * shapeless instead produces {@code patchouli:guide_book} carrying the {@code patchouli:book} data component
     * (a {@link net.minecraft.resources.ResourceLocation} value - see {@code PatchouliDataComponents.BOOK}), which
     * opens this exact book.
     */
    private JsonObject bookRecipe() {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "minecraft:crafting_shapeless");
        JsonArray ingredients = new JsonArray();
        ingredients.add(ingredient("minecraft:book"));
        ingredients.add(ingredient(RCC.MODID + ":compression_interface"));
        recipe.add("ingredients", ingredients);

        JsonObject result = new JsonObject();
        result.addProperty("id", "patchouli:guide_book");
        result.addProperty("count", 1);
        JsonObject components = new JsonObject();
        components.addProperty("patchouli:book", RCC.MODID + ":" + BOOK);
        result.add("components", components);
        recipe.add("result", result);
        return recipe;
    }

    private static JsonObject ingredient(String item) {
        JsonObject obj = new JsonObject();
        obj.addProperty("item", item);
        return obj;
    }

    private JsonObject category(String name, String description, String icon) {
        JsonObject cat = new JsonObject();
        cat.addProperty("name", name);
        cat.addProperty("description", description);
        cat.addProperty("icon", icon);
        return cat;
    }

    private static String item(String name) {
        return RCC.MODID + ":" + name;
    }

    // --- Entry content -------------------------------------------------------------------------

    /** One entry's location + body. */
    private record Entry(String folder, String file, JsonObject json) {
    }

    private List<Entry> introEntries() {
        List<Entry> list = new ArrayList<>();

        List<JsonObject> bcePages = new ArrayList<>();
        bcePages.addAll(textPages(null,
                "$(b)BCE$() - Base Cobblestone Equivalent - is the mod's one currency of mass. One vanilla cobblestone is"
                        + " exactly $(#00AA00)1 BCE$(), and every number in this mod is that many cobblestones compressed"
                        + " down to a single block."));
        bcePages.addAll(textPages("The 9^n ladder",
                "Compress nine blocks into one and you have tier 1, worth $(l)9$() BCE. Tier $(#00AA00)n$() is worth"
                        + " $(l)9^n$() cobblestones, so value explodes while the tier number stays small. Tiers ride on"
                        + " the item itself, so the ladder has no ceiling."));
        bcePages.addAll(textPages("Notation",
                "Big figures are written as power towers such as $(l)9^(10^12)$(). Machine tooltips draw these raised,"
                        + " and the numbers are kept in $(#00AA00)log10$() space internally so even a googol tier stays"
                        + " a finite, comparable value."));
        list.add(new Entry("intro", "what_is_bce.json",
                entry("Base Cobblestone Equivalents", item("compressed_cobblestone"), "intro", bcePages)));

        List<JsonObject> shellPages = new ArrayList<>();
        shellPages.addAll(textPages(null,
                "Every tier-1 Manipulator is a hollow box of four reusable blocks. Learn these and you can read any"
                        + " structure in this book at a glance."));
        shellPages.add(craftPage("Compression Frame", "The solid edge and corner of every shell.", "compression_frame"));
        shellPages.add(craftPage("Compression Glass", "A transparent face panel. The sealed core cannot be clicked, so light reaches it only through these.", "compression_glass"));
        shellPages.add(craftPage("Compression Port", "The shell's connection face. Cycle a placed port with the wrench between $(o)Power$(), $(o)Item Input$() and $(o)Item Output$().", "compression_port"));
        shellPages.add(craftPage("Interface", "The one GUI entry per structure - right-click it. Exactly one is required so there is never an ambiguous panel.", "compression_interface"));
        list.add(new Entry("intro", "shell_blocks.json",
                entry("The Shared Shell", item("compression_frame"), "intro", shellPages)));

        return list;
    }

    private List<Entry> band0Entries() {
        List<Entry> list = new ArrayList<>();

        List<JsonObject> manualPages = new ArrayList<>();
        manualPages.addAll(textPages(null,
                "Craft a 3x3 grid of a tier to get one block of the tier above, or a single block on its own to spread"
                        + " it back into nine below. This hand chain runs to $(l)tier 1000$(), where the Compression"
                        + " Manipulator takes over - and it tops out there because a crafting table cannot hold the"
                        + " numbers past it."));
        manualPages.addAll(textPages("Fuel",
                "Compressed cobblestone is also fuel. Almost every machine burns BCE to work, so the blocks you make"
                        + " double as the mass that powers making more of them."));
        list.add(new Entry("band0", "manual_chain.json",
                entry("The Manual Climb", item("compressed_cobblestone"), "band0", manualPages)));

        List<JsonObject> autoCompPages = new ArrayList<>();
        autoCompPages.add(craftPage("Auto-Compressor",
                "Feed cobblestone in and it runs the 3x3 compression for you, up to the tier-1000 ceiling.",
                "compression_auto"));
        autoCompPages.addAll(textPages(null,
                "It accepts plain vanilla cobblestone as a tier-0 seed, so it bootstraps from nothing. Burn BCE as fuel"
                        + " and the more it runs, the more it powers itself."));
        list.add(new Entry("band0", "auto_compressor.json",
                entry("Auto-Compressor", item("compression_auto"), "band0", autoCompPages)));

        List<JsonObject> autoDecompPages = new ArrayList<>();
        autoDecompPages.add(craftPage("Auto Decompressor",
                "Feed a compressed block in, set a goal tier, and it steps it back down - banking the released mass as"
                        + " raw BCE.",
                "compression_auto_decompressor"));
        autoDecompPages.addAll(textPages(null,
                "It ejects the downgraded block too, and it is the mod's first real way to turn stored cobblestone back"
                        + " into BCE."));
        list.add(new Entry("band0", "auto_decompressor.json",
                entry("Auto Decompressor", item("compression_auto_decompressor"), "band0", autoDecompPages)));

        List<JsonObject> genPages = new ArrayList<>();
        genPages.addAll(textPages("The Ladder",
                "The free tier-0 drip that bootstraps everything, driven by one block's $(o)tier$() state. Low rungs spit"
                        + " cobblestone items - take them with a hopper or item pipe. High rungs mint raw BCE straight"
                        + " onto the $9^n$ curve - drain them with an extract-mode BCE pipe."));
        genPages.addAll(textPages("Rung Recipes",
                "Each rung is gated by the cobblestone milestone it needs. This is the $(o)Base$() rung - the entry to"
                        + " the ladder. Every higher rung is its own recipe ($(l)cobblestone_generator_<rung>$()); see"
                        + " JEI for the full ladder."));
        genPages.add(craftPage("Cobblestone Gen.", "Craft the Base rung to start the ladder.",
                "cobblestone_generator_base"));
        list.add(new Entry("band0", "generator.json",
                entry("Cobblestone Generator", item("cobblestone_generator"), "band0", genPages)));

        return list;
    }

    /**
     * The three Manipulator entries for one band, rendered from {@code w}'s prose plus the band's live geometry. Called
     * for every {@link #BANDS} row, so band 1 and band 2 share one code path - the only per-band inputs are the
     * editorial strings in the write-up and the {@link CompressionBand} the previews read their shape from.
     */
    private List<Entry> manipulatorEntries(BandWriteup w) {
        List<Entry> list = new ArrayList<>();
        String suffix = w.coreLabelSuffix();

        // Compression Manipulator: the band's compressor cube, doubling the seed tier.
        List<JsonObject> compPages = new ArrayList<>();
        compPages.addAll(textPages("What it does", w.compDoes()));
        compPages.addAll(textPages("What it needs",
                "Like every Manipulator it wants a complete shell, FE pushed through a Power port, and enough compressed"
                        + " cobblestone burned as fuel."));
        compPages.addAll(textPages(w.compShellTitle(),
                "A hollow cube of frame on every edge and corner with glass across the faces, and air inside. It carries"
                        + " at least one Power port, one Item-Input and one Item-Output, exactly one Interface, and a"
                        + " Compression Core sealing the dead centre."));
        compPages.add(multiblockPage("Compression Manipulator", "The live layout, generated straight from the band table.", w.band(), Kind.COMPRESSOR));
        compPages.add(craftPage("Compression Core" + suffix, "The sealed heart of the structure - place it last so the shell's ports wake up.", w.compressorItem()));
        list.add(new Entry(w.folder(), "compression_manipulator.json",
                entry("Compression Manipulator", item(w.compressorItem()), w.folder(), compPages)));

        // Decompression Manipulator: the band's descent cube, stepping down.
        List<JsonObject> decompPages = new ArrayList<>();
        decompPages.addAll(textPages("What it does", w.decompDoes()));
        decompPages.addAll(textPages(w.decompShellTitle(),
                "A hollow cube from the same shell vocabulary as its compressor cousin: frame edges, glass faces, one"
                        + " Interface, the ports it needs, and a Decompression Core at the centre."));
        decompPages.add(multiblockPage("Decompression Manipulator", "The live layout, generated straight from the band table.", w.band(), Kind.DECOMPRESSOR));
        decompPages.add(craftPage("Decompression Core" + suffix, "Mirrors the Compression Core; unlocked once you own one.", w.decompressorItem()));
        list.add(new Entry(w.folder(), "decompression_manipulator.json",
                entry("Decompression Manipulator", item(w.decompressorItem()), w.folder(), decompPages)));

        // Generation Manipulator: the band's FE-priced mint tower.
        List<JsonObject> genPages = new ArrayList<>();
        genPages.addAll(textPages("What it does", w.genDoes()));
        genPages.addAll(textPages(null,
                "Unlike the free drip it $(#AA0000)costs FE$() while minting, so power infrastructure becomes the"
                        + " progression gate."));
        genPages.addAll(textPages("Array Stacking",
                "One tower only mints its rung's $(l)share$(); what a bus mints is the $(l)sum of the exponents$(). Join"
                        + " $(l)2^(band+1)$() top-rung towers on one BCE bus and together they mint a whole band-max block"
                        + " per second."));
        genPages.addAll(textPages("Sharing walls",
                "Same-band towers may share a wall - and a shared Power port splits its incoming FE across both owners (a"
                        + " split, not a discount)."));
        genPages.addAll(textPages(w.genTowerTitle(),
                "The core seals one of the two interior slots of the centre column, leaving a 1x1 air shaft in the other"
                        + " (core-up and core-down are both valid). Frame edges, glass faces, at least one Power port,"
                        + " exactly one Item-Output (the BCE-out drain) and exactly one Interface as the status panel."));
        genPages.add(multiblockPage("Generation Manipulator", "The live layout, generated straight from the band table.", w.band(), Kind.GENERATOR));
        genPages.add(craftPage("Generator Core" + suffix, "The rung is chosen by the item's block_state; climb the ladder one rung per craft.", w.generatorItem()));
        list.add(new Entry(w.folder(), "generation_manipulator.json",
                entry("Generation Manipulator", item(w.generatorItem()), w.folder(), genPages)));

        return list;
    }

    private List<Entry> logisticsEntries() {
        List<Entry> list = new ArrayList<>();

        List<JsonObject> pipePages = new ArrayList<>();
        pipePages.add(craftPage("BCE Pipe",
                "Carries raw BCE between machines; the pipe's tier sets the per-transfer ceiling.",
                "bce_pipe_one"));
        pipePages.addAll(textPages(null,
                "Connected segments form one bus, and each end is pointed with the wrench ($(o)Extract$() /"
                        + " $(o)Insert$() / $(o)Passive$()). A line can never move more than one whole band-max block at"
                        + " a time. A generator's BCE-out drains into the bus; a tank or a machine's fuel port takes it"
                        + " back out."));
        list.add(new Entry("logistics", "bce_pipe.json",
                entry("BCE Pipe", item("bce_pipe"), "logistics", pipePages)));

        List<JsonObject> storagePages = new ArrayList<>();
        storagePages.addAll(textPages(null,
                "A passive BCE tank. Its flow is driven by the pipes touching it - fill it with an extract pipe from any"
                        + " BCE source, drain it with an insert pipe into any sink. Each tier is sized to hold one whole"
                        + " band-max block's BCE."));
        storagePages.add(craftPage("BCE Storage", "A frame shell around one interface panel.", "bce_storage"));
        storagePages.add(craftPage("BCE Storage II", "The band-1 tank - one full 10^15-tier block of BCE.", "bce_storage_2"));
        list.add(new Entry("logistics", "bce_storage.json",
                entry("BCE Storage", item("bce_storage"), "logistics", storagePages)));

        List<JsonObject> wrenchPages = new ArrayList<>();
        wrenchPages.add(spotlight("The Wrench",
                "Right-click a placed port to cycle its kind, or a pipe end to flip that end's direction.",
                item("wrench")));
        wrenchPages.addAll(textPages("Cross-Mod",
                "The wrench is published onto the shared $(o)c:tools/wrench$() tag, so other mods' wrenches work on our"
                        + " machines and this one works on theirs."));
        wrenchPages.add(craftPage("Compression Wrench", "One wrench configures every port and pipe end.",
                "wrench"));
        list.add(new Entry("logistics", "wrench.json",
                entry("Compression Wrench", item("wrench"), "logistics", wrenchPages)));

        return list;
    }

    // --- Page builders -------------------------------------------------------------------------

    private static JsonObject textPage(String title, String text) {
        JsonObject page = new JsonObject();
        page.addProperty("type", "patchouli:text");
        if (title != null) {
            page.addProperty("title", title);
        }
        page.addProperty("text", text);
        return page;
    }

    /**
     * Lay a long body out over as many {@link #textPage} pages as it needs. Sentences are packed up to
     * {@link #MAX_PAGE_CHARS}; an over-long single sentence is force-wrapped on whitespace. Patchouli macros
     * ({@code $(l)$()} etc.) contain no spaces, so the word-level fallback can never split one. Only the first page
     * carries {@code title}; continuations are untitled.
     */
    private static List<JsonObject> textPages(String title, String body) {
        List<String> pages = new ArrayList<>();
        for (String sentence : body.split("(?<=\\.)\\s+")) {
            for (String piece : forceWrap(sentence)) {
                int last = pages.size() - 1;
                if (last >= 0 && visibleLen(pages.get(last) + " " + piece) <= MAX_PAGE_CHARS) {
                    pages.set(last, pages.get(last) + " " + piece);
                } else {
                    pages.add(piece);
                }
            }
        }
        List<JsonObject> out = new ArrayList<>();
        for (int i = 0; i < pages.size(); i++) {
            out.add(textPage(i == 0 ? title : null, pages.get(i)));
        }
        return out;
    }

    private static List<String> forceWrap(String sentence) {
        List<String> parts = new ArrayList<>();
        if (visibleLen(sentence) <= MAX_PAGE_CHARS) {
            parts.add(sentence);
            return parts;
        }
        StringBuilder cur = new StringBuilder();
        for (String word : sentence.split("\\s+")) {
            if (cur.length() > 0 && visibleLen(cur + " " + word) > MAX_PAGE_CHARS) {
                parts.add(cur.toString());
                cur.setLength(0);
            }
            if (cur.length() > 0) {
                cur.append(' ');
            }
            cur.append(word);
        }
        if (cur.length() > 0) {
            parts.add(cur.toString());
        }
        return parts;
    }

    /** Length as Patchouli will render it - formatting macros such as {@code $(l)...$()} are stripped first. */
    private static int visibleLen(String text) {
        return MACRO.matcher(text).replaceAll("").length();
    }

    /** A two-column crafting page showing the single recipe with the given id. */
    private static JsonObject craftPage(String title, String text, String recipePath) {
        JsonObject page = new JsonObject();
        page.addProperty("type", "patchouli:crafting");
        if (title != null) {
            page.addProperty("title", title);
        }
        if (text != null) {
            page.addProperty("text", text);
        }
        page.addProperty("recipe", RCC.MODID + ":" + recipePath);
        return page;
    }

    private static JsonObject spotlight(String title, String text, String item) {
        JsonObject page = new JsonObject();
        page.addProperty("type", "patchouli:spotlight");
        page.addProperty("title", title);
        page.addProperty("text", text);
        page.addProperty("item", item);
        return page;
    }

    /** A multiblock page whose pattern/mapping are computed live from {@code band} and {@code kind}. */
    private JsonObject multiblockPage(String name, String text, CompressionBand band, Kind kind) {
        JsonObject page = new JsonObject();
        page.addProperty("type", "patchouli:multiblock");
        page.addProperty("name", name);
        page.addProperty("text", text);
        page.add("multiblock", multiblockObject(band, kind));
        return page;
    }

    private JsonObject entry(String name, String icon, String category, List<JsonObject> pages) {
        JsonObject entry = new JsonObject();
        entry.addProperty("name", name);
        entry.addProperty("icon", icon);
        entry.addProperty("category", RCC.MODID + ":" + category);
        JsonArray arr = new JsonArray();
        pages.forEach(arr::add);
        entry.add("pages", arr);
        return entry;
    }

    private CompletableFuture<?> save(CachedOutput cache, Path path, JsonElement json) {
        return DataProvider.saveStable(cache, json, path);
    }

    // --- The generated geometry --------------------------------------------------------------

    /** The three band-1 structures, each with the core that seals its centre / column. */
    private enum Kind {
        COMPRESSOR,
        DECOMPRESSOR,
        GENERATOR
    }

    /**
     * Build Patchouli's {@code { pattern, mapping, symmetrical }} object for one structure entirely from the band
     * row: side from the radius, height from the radius (cubes) or the tower's layer counts, the core id, and the
     * role of every cell. Nothing here is a hand-typed literal the scanner could disagree with.
     */
    private JsonObject multiblockObject(CompressionBand band, Kind kind) {
        int width;
        int height;
        boolean symmetrical;
        String coreId;
        int coreLayerFromTop;
        switch (kind) {
            case COMPRESSOR -> {
                int r = band.compressorRadius();
                width = 2 * r + 1;
                height = width;
                symmetrical = true;
                coreId = block(coreBlockFor(band, kind));
                coreLayerFromTop = height / 2;
            }
            case DECOMPRESSOR -> {
                int r = band.decompressorRadius();
                width = 2 * r + 1;
                height = width;
                symmetrical = true;
                coreId = block(coreBlockFor(band, kind));
                coreLayerFromTop = height / 2;
            }
            case GENERATOR -> {
                int half = band.generatorHalfWidth();
                width = 2 * half + 1;
                height = band.generatorHeight();
                symmetrical = false; // the tower's vertical axis is meaningful (core vs air shaft)
                coreId = block(coreBlockFor(band, kind));
                coreLayerFromTop = height - 1 - band.generatorLayersBelow();
            }
            default -> throw new IllegalStateException(kind.name());
        }

        int cx = width / 2;
        int cy = height / 2;
        // The generator takes no items - its scanner refuses an ITEM_IN port outright, so the preview must not draw
        // one (that face cell then falls through to ordinary glass). The two cubes both require a fuel ITEM_IN port.
        boolean takesItemIn = kind != Kind.GENERATOR;

        JsonArray pattern = new JsonArray();
        for (int iy = 0; iy < height; iy++) { // top -> bottom
            JsonArray layer = new JsonArray();
            for (int iz = 0; iz < width; iz++) { // rows: north -> south
                StringBuilder row = new StringBuilder(width);
                for (int ix = 0; ix < width; ix++) { // columns: west -> east
                    row.append(cellChar(ix, iy, iz, cx, cy, width, height, coreLayerFromTop, takesItemIn));
                }
                layer.add(row.toString());
            }
            pattern.add(layer);
        }

        JsonObject multiblock = new JsonObject();
        multiblock.add("pattern", pattern);
        multiblock.add("mapping", mapping(coreId));
        multiblock.addProperty("symmetrical", symmetrical);
        return multiblock;
    }

    /**
     * The character for one cell. Face-centre roles are forced first (one Interface, one of each port kind, the core
     * marker), then the boundary count decides frame (2+), glass (exactly 1) or air (interior / the tower's shaft).
     */
    private static char cellChar(int ix, int iy, int iz, int cx, int cy, int width, int height, int coreLayerFromTop,
            boolean takesItemIn) {
        if (ix == cx && iz == cx && iy == coreLayerFromTop) {
            return '0'; // the core seals the centre (cube) / its slot (tower)
        }
        if (iy == 0 && ix == cx && iz == cx) {
            return 'P'; // Power port caps the top face centre
        }
        if (iy == cy && iz == 0 && ix == cx) {
            return 'I'; // the one Interface on a side face centre
        }
        if (takesItemIn && iy == cy && ix == 0 && iz == cx) {
            return 'Q'; // Item-Input (fuel) on the west face centre - the two cubes only
        }
        if (iy == cy && ix == width - 1 && iz == cx) {
            return 'R'; // Item-Output (the BCE-out drain) on the east face centre
        }
        int boundary = 0;
        if (ix == 0 || ix == width - 1) boundary++;
        if (iz == 0 || iz == width - 1) boundary++;
        if (iy == 0 || iy == height - 1) boundary++;
        if (boundary >= 2) return 'F'; // edge / corner
        if (boundary == 1) return 'G'; // face panel
        return ' ';                    // interior air / the tower's shaft
    }

    /**
     * The core block that seals {@code band}'s structure of {@code kind}. The preview's {@code 0} mapping is the one
     * piece a band cannot derive from geometry alone, so it is looked up here: band 1 is the shipped line, band 2 the
     * {@code _2} cores, and a later band resolves to its own registration the same way.
     */
    private static net.minecraft.world.level.block.Block coreBlockFor(CompressionBand band, Kind kind) {
        boolean secondBand = band == CompressionBand.BAND_2;
        return switch (kind) {
            case COMPRESSOR -> secondBand ? ModBlocks.COMPRESSION_CORE_2.get() : ModBlocks.CORE.get();
            case DECOMPRESSOR ->
                    secondBand ? ModBlocks.COMPRESSION_DECOMPRESSOR_CORE_2.get() : ModBlocks.COMPRESSION_DECOMPRESSOR_CORE.get();
            case GENERATOR -> secondBand ? ModBlocks.GENERATOR_CORE_2.get() : ModBlocks.GENERATOR_CORE.get();
        };
    }

    private JsonObject mapping(String coreId) {
        JsonObject map = new JsonObject();
        map.addProperty("F", block(ModBlocks.FRAME.get()));
        map.addProperty("G", block(ModBlocks.GLASS.get()));
        map.addProperty("I", block(ModBlocks.INTERFACE.get()));
        map.addProperty("P", portState("power"));
        map.addProperty("Q", portState("item_in"));
        map.addProperty("R", portState("item_out"));
        map.addProperty("0", coreId);
        return map;
    }

    private static String block(net.minecraft.world.level.block.Block block) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    private static String portState(String portType) {
        return block(ModBlocks.PORT.get()) + "[port_type=" + portType + "]";
    }
}
