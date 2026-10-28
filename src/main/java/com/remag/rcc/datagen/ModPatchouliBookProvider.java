package com.remag.rcc.datagen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.remag.rcc.RCC;
import com.remag.rcc.block.ModBlocks;
import com.remag.rcc.registry.CompressionBand;
import net.minecraft.core.Direction;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class ModPatchouliBookProvider implements DataProvider {
    private static final String BOOK = "rcc_manual";
    private static final String LANG = "en_us";
    private static final int MAX_PAGE_CHARS = 500;
    private static final Pattern MACRO = Pattern.compile("\\$\\([^)]*\\)");

    private final PackOutput output;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private record BandWriteup(
            CompressionBand band,
            String folder,
            String categoryName,
            String categoryDesc,
            String compressorItem,
            String decompressorItem,
            String generatorItem,
            String coreLabelSuffix,
            String compDoes,
            String compShellTitle,
            String decompDoes,
            String decompShellTitle,
            String genDoes,
            String genTowerTitle) {
    }

    private static final List<BandWriteup> BANDS = List.of(
            new BandWriteup(CompressionBand.BAND_1, "band1",
                    "Tier 1 - Manipulators",
                    "The first three multiblock machines. All are built from one shared shell.",
                    "compression_core", "compression_decompressor_core", "generator_core", "",
                    "Takes a seed of at least $(l)tier 1000$() and doubles its tier: a tier $(#00AA00)n$() block becomes a"
                            + " tier $(#00AA00)2n$() block. The result can rise to this band's ceiling of $(l)10^15$().",
                    "5x5x5 Shell",
                    "The multiblock version of the Auto Decompressor. Set a goal tier and it lowers a large block one tier"
                            + " at a time, releasing the BCE it no longer needs.",
                    "3x3x3 Shell",
                    "A generator column, four tall and three wide, with a Generator Core in one of its two interior slots."
                            + " It produces BCE at the 9^n rate, from a $(l)tier-1000$() block's worth per second up to"
                            + " half a $(l)tier-10^15$() block.",
                    "3x3x4 Tower"),
            new BandWriteup(CompressionBand.BAND_2, "band2",
                    "Tier 2 - Manipulators",
                    "The same three machines, built at larger sizes.",
                    "compression_core_2", "compression_decompressor_core_2", "generator_core_2", " II",
                    "Takes a seed of at least $(l)tier 10^15$() and doubles its tier: a tier $(#00AA00)n$() block becomes a"
                            + " tier $(#00AA00)2n$() block. The result can rise to this band's ceiling of $(l)10^27$().",
                    "7x7x7 Shell",
                    "The multiblock version of the Auto Decompressor. Set a goal tier and it lowers a large block one tier"
                            + " at a time, releasing the BCE it no longer needs.",
                    "5x5x5 Shell",
                    "A generator column, six tall and five wide, with a Generator Core in one of its two interior slots."
                            + " It produces BCE at the 9^n rate, from a $(l)tier-10^15$() block's worth per second up to"
                            + " half a $(l)tier-10^27$() block.",
                    "5x5x6 Tower"),
            bandWriteup(CompressionBand.BAND_3, "Manipulators",
                    "Nine-wide shells. This is the last band with a decompressor, and its tower is ten tall."),
            bandWriteup(CompressionBand.BAND_4, "Manipulators",
                    "Compressor and tower only from here on; decomposition stays with band 3."),
            bandWriteup(CompressionBand.BAND_5, "Manipulators",
                    "Thirteen-wide shells."),
            bandWriteup(CompressionBand.BAND_6, "Manipulators",
                    "Fifteen-wide shells and a fourteen-tall tower."),
            bandWriteup(CompressionBand.BAND_7, "Manipulators",
                    "Seventeen-wide shells."),
            bandWriteup(CompressionBand.BAND_8, "Manipulators",
                    "Nineteen-wide shells, the highest band in the mod."));

    public ModPatchouliBookProvider(PackOutput output) {
        this.output = output;
    }

    private static BandWriteup bandWriteup(CompressionBand band, String flavour, String categoryDesc) {
        int n = band.index();
        String minTier = tierText(band.minInputTier());
        String maxTier = tierText(band.maxOutputTier());
        String compSide = cubeSideText(band.compressorRadius());
        String decompSide = cubeSideText(band.decompressorRadius());
        String towerTitle = cubeSideText(band.generatorHalfWidth()) + "x" + band.generatorHeight() + " Tower";
        String decompItem = band.hasDecompressor() ? "compression_decompressor_core_" + n : null;
        return new BandWriteup(band, "band" + n,
                "Tier " + n + " - " + flavour,
                categoryDesc,
                "compression_core_" + n, decompItem, "generator_core_" + n,
                " " + romanUpper(n),
                "Takes a seed of at least $(l)tier " + minTier + "$() and doubles its tier: a tier"
                        + " $(#00AA00)n$() block becomes a tier $(#00AA00)2n$() block. The result can rise to this"
                        + " band's ceiling of $(l)" + maxTier + "$().",
                compSide + " Shell",
                "The multiblock version of the Auto Decompressor. Set a goal tier and it lowers a large block one tier"
                        + " at a time, releasing the BCE it no longer needs.",
                decompSide + " Shell",
                "A generator column, " + band.generatorHeight() + " tall and "
                        + (2 * band.generatorHalfWidth() + 1) + " wide, with a Generator Core in one of its two"
                        + " interior slots. It produces BCE at the 9^n rate, from a $(l)tier-" + minTier + "$() block's"
                        + " worth per second up to half a $(l)tier-" + maxTier + "$() block.",
                towerTitle);
    }

    private static String tierText(BigInteger tier) {
        BigInteger ten = BigInteger.TEN;
        int exponent = 0;
        BigInteger reduced = tier;
        while (reduced.mod(ten).signum() == 0 && reduced.signum() > 0) {
            reduced = reduced.divide(ten);
            exponent++;
        }
        if (exponent > 0 && reduced.equals(BigInteger.ONE)) {
            return "10^" + exponent;
        }
        return tier.toString();
    }

    private static String cubeSideText(int radius) {
        int side = 2 * radius + 1;
        return side + "x" + side + "x" + side;
    }

    private static String romanUpper(int number) {
        String[] numerals = {"VIII", "VII", "VI", "V", "IV", "III", "II", "I"};
        int[] values = {8, 7, 6, 5, 4, 3, 2, 1};
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < values.length && number >= values[i]; i++) {
            out.append(numerals[i]);
            number -= values[i];
        }
        return out.toString();
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

        futures.add(save(cache, bookData.resolve("book.json"), bookJson()));
        futures.add(save(cache, dataRoot.resolve("recipe").resolve(BOOK + ".json"), bookRecipe()));

        futures.add(save(cache, bookAssets.resolve("categories").resolve("intro.json"),
                category("Getting Started", "How this mod works and what BCE is.", item("compressed_cobblestone"))));
        futures.add(save(cache, bookAssets.resolve("categories").resolve("band0.json"),
                category("Tier 0 - Single Blocks", "Manual crafting and the first single-block machines.", item("compression_auto"))));
        for (BandWriteup w : BANDS) {
            futures.add(save(cache, bookAssets.resolve("categories").resolve(w.folder() + ".json"),
                    category(w.categoryName(), w.categoryDesc(), item(w.compressorItem()))));
        }
        futures.add(save(cache, bookAssets.resolve("categories").resolve("logistics.json"),
                category("Logistics", "Moving and storing BCE between machines.", item("bce_pipe"))));

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

    private JsonObject bookJson() {
        JsonObject book = new JsonObject();
        book.addProperty("name", "RCC Manual");
        book.addProperty("author", "Remag");
        book.addProperty("landing_text",
                "Welcome to RCC, a mod about generating insane quantities of cobblestone.");
        book.addProperty("text_overflow_mode", "resize");
        book.addProperty("version", 1);
        book.addProperty("use_resource_pack", true);
        book.addProperty("creative_tab", RCC.MODID + ":rcc_machines_tab");
        book.addProperty("show_progress", false);
        book.addProperty("read_only", false);
        return book;
    }

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

    private record Entry(String folder, String file, JsonObject json) {
    }

    private List<Entry> introEntries() {
        List<Entry> list = new ArrayList<>();

        List<JsonObject> bcePages = new ArrayList<>();
        bcePages.addAll(textPages(null,
                "$(l)BCE$() (Base Cobblestone Equivalent) is the unit of mass used throughout the mod. One vanilla"
                        + " cobblestone equals exactly $(#00AA00)1 BCE$(). Every large number here represents that many"
                        + " cobblestones compressed into a single block."));
        bcePages.addAll(textPages("The 9^n Progression",
                "Nine blocks of a tier craft into one tier-1 block, worth $(l)9$() BCE. In general a tier $(#00AA00)n$()"
                        + " block is worth $(l)9^n$() cobblestones, so the value grows far faster than the tier number."));
        bcePages.addAll(textPages("Notation",
                "Very large values are written as power towers such as $(l)9^(10^12)$(). Tooltips render these as"
                        + " superscripts."));
        list.add(new Entry("intro", "what_is_bce.json",
                entry("Base Cobblestone Equivalents", item("compressed_cobblestone"), "intro", bcePages)));

        List<JsonObject> shellPages = new ArrayList<>();
        shellPages.addAll(textPages(null,
                "Every Manipulator is a hollow shell built from four reusable blocks. Once you know these, you can read"
                        + " any structure in this book."));
        shellPages.add(craftPage("Compression Frame", "Forms the edges and corners of every shell.", "compression_frame"));
        shellPages.add(craftPage("Compression Glass", "A transparent panel that fills the flat faces of the shell between its frame edges.", "compression_glass"));
        shellPages.add(craftPage("Compression Port", "The shell's connection block. Only the coloured face transfers anything, so point it at the external cable and not into the shell. Right-click a placed port with a wrench to cycle it between $(o)Power$(), $(o)Item Input$(), $(o)Item Output$(), and $(o)Seed$().", "compression_port"));
        shellPages.add(craftPage("Interface", "The structure's GUI. Right-click it to open the machine. Exactly one Interface is required per structure.", "compression_interface"));
        list.add(new Entry("intro", "shell_blocks.json",
                entry("Shell Blocks", item("compression_frame"), "intro", shellPages)));

        return list;
    }

    private List<Entry> band0Entries() {
        List<Entry> list = new ArrayList<>();

        List<JsonObject> manualPages = new ArrayList<>();
        manualPages.addAll(textPages(null,
                "Craft a 3x3 grid of a tier to make one block of the tier above, or craft a single block on its own to"
                        + " break it back into nine of the tier below. This manual chain runs to $(l)tier 1000$(), where"
                        + " the Compression Manipulator takes over."));
        manualPages.addAll(textPages("Fuel",
                "Compressed cobblestone is also a fuel. Most machines burn BCE to run, so the blocks you produce supply"
                        + " the power to make more of them."));
        list.add(new Entry("band0", "manual_chain.json",
                entry("Manual Crafting", item("compressed_cobblestone"), "band0", manualPages)));

        List<JsonObject> autoCompPages = new ArrayList<>();
        autoCompPages.add(craftPage("Auto-Compressor",
                "Takes cobblestone as input and performs the 3x3 compression automatically, up to the tier-1000 ceiling.",
                "compression_auto"));
        autoCompPages.addAll(textPages(null,
                "It accepts plain vanilla cobblestone as a tier-0 seed, so it can start from nothing. It burns BCE as"
                        + " fuel, so the more it runs, the more of its own fuel it produces."));
        list.add(new Entry("band0", "auto_compressor.json",
                entry("Auto-Compressor", item("compression_auto"), "band0", autoCompPages)));

        List<JsonObject> autoDecompPages = new ArrayList<>();
        autoDecompPages.add(craftPage("Auto Decompressor",
                "Insert a compressed block, set a goal tier, and it lowers it one tier at a time, storing the released"
                        + " mass as raw BCE.",
                "compression_auto_decompressor"));
        autoDecompPages.addAll(textPages(null,
                "It also ejects the downgraded block. This is the first way to convert stored cobblestone back into"
                        + " usable BCE."));
        list.add(new Entry("band0", "auto_decompressor.json",
                entry("Auto Decompressor", item("compression_auto_decompressor"), "band0", autoDecompPages)));

        List<JsonObject> genPages = new ArrayList<>();
        genPages.addAll(textPages("Generator Tiers",
                "The free tier-0 source that starts the whole progression. Low"
                        + " tiers output cobblestone items, collected with a hopper or item pipe. High tiers output raw"
                        + " BCE directly on the 9^n curve, drained with an extract-mode BCE pipe."));
        genPages.addAll(textPages("Generator Recipes",
                "Each tier is unlocked by the cobblestone milestone it requires. This is the $(o)Base$() tier, the entry"
                        + " point. Every higher tier is its own recipe; see JEI"
                        + " for the full list."));
        genPages.add(craftPage("Cobblestone Gen.", "Craft the Base tier to begin the progression.",
                "cobblestone_generator_base"));
        list.add(new Entry("band0", "generator.json",
                entry("Cobblestone Generator", item("cobblestone_generator"), "band0", genPages)));

        return list;
    }

    private List<Entry> manipulatorEntries(BandWriteup w) {
        List<Entry> list = new ArrayList<>();
        String suffix = w.coreLabelSuffix();

        List<JsonObject> compPages = new ArrayList<>();
        compPages.addAll(textPages("What it does",
                w.compDoes()
                        + " Like the other Manipulators it needs a complete shell, FE through a Power port, and"
                        + " compressed cobblestone burned as fuel."));
        compPages.addAll(textPages(w.compShellTitle(),
                "A hollow cube of frame along every edge and corner, glass across the faces, and air inside. It uses at"
                        + " least one Power port, one Item-Input and one Item-Output, exactly one Interface, and a"
                        + " Compression Core at the centre. An optional $(l)Seed$() port lets a pipe feed the seed slot,"
                        + " so an automated line can run unattended."
                        + powerPortAdvice(w.band())));
        compPages.add(multiblockPage("Compression Manipulator", "The live layout, generated from the band table.", w.band(), Kind.COMPRESSOR));
        compPages.add(craftPage("Compression Core" + suffix, "The sealed centre of the structure. Place it last so the shell's ports activate.", w.compressorItem()));
        list.add(new Entry(w.folder(), "compression_manipulator.json",
                entry("Compression Manipulator", item(w.compressorItem()), w.folder(), compPages)));

        if (w.decompressorItem() != null) {
            List<JsonObject> decompPages = new ArrayList<>();
            decompPages.addAll(textPages("What it does", w.decompDoes()));
            decompPages.addAll(textPages(w.decompShellTitle(),
                    "A hollow cube built from the same shell blocks as the compressor: frame edges, glass faces, one"
                            + " Interface, the ports it needs, and a Decompression Core at the centre."
                            + powerPortAdvice(w.band())));
            decompPages.add(multiblockPage("Decompression Manipulator", "The live layout, generated from the band table.", w.band(), Kind.DECOMPRESSOR));
            decompPages.add(craftPage("Decompression Core" + suffix, "Works like the Compression Core, and unlocks once you have one.", w.decompressorItem()));
            list.add(new Entry(w.folder(), "decompression_manipulator.json",
                    entry("Decompression Manipulator", item(w.decompressorItem()), w.folder(), decompPages)));
        }

        List<JsonObject> genPages = new ArrayList<>();
        genPages.addAll(textPages("What it does", w.genDoes()));
        genPages.addAll(textPages("Output and Power",
                "Unlike the free tier-0 generator, it $(#AA0000)costs FE$() while producing, so power supply becomes the"
                        + " limiting factor. A single tower produces only its own rung's $(l)share$(); what a bus"
                        + " produces is the $(l)sum of the exponents$(). Joining $(l)2^(band+1)$() top-rung towers on one"
                        + " BCE bus produces a whole band-max block per second. Towers of the same band may share a wall,"
                        + " and a shared Power port splits its incoming FE between both owners (a split, not a"
                        + " discount)."));
        genPages.addAll(textPages(w.genTowerTitle(),
                "The core fills one of the two interior slots of the centre column, leaving a 1x1 air shaft in the other"
                        + " (core-up and core-down are both valid). The tower uses frame edges, glass faces, at least one"
                        + " Power port, exactly one Item-Output (the BCE-out drain), and exactly one Interface as the"
                        + " status panel."
                        + powerPortAdvice(w.band())));
        genPages.add(multiblockPage("Generation Manipulator", "The live layout, generated from the band table.", w.band(), Kind.GENERATOR));
        genPages.add(craftPage("Generator Core" + suffix, "Each craft advances the generator one rung up the ladder.", w.generatorItem()));
        list.add(new Entry(w.folder(), "generation_manipulator.json",
                entry("Generation Manipulator", item(w.generatorItem()), w.folder(), genPages)));

        return list;
    }

    private List<Entry> logisticsEntries() {
        List<Entry> list = new ArrayList<>();

        List<JsonObject> pipePages = new ArrayList<>();
        pipePages.add(craftPage("BCE Pipe",
                "Carries BCE between machines. The pipe's tier sets the maximum transferred per operation.",
                "bce_pipe_one"));
        pipePages.addAll(textPages(null,
                "Connected segments form a single bus. Set each end with the wrench to $(o)Extract$(), $(o)Insert$(), or"
                        + " $(o)Passive$(). One line moves at most one band-max block at a time. A generator's"
                        + " BCE-out flows into the bus, and a tank or machine's fuel port draws it out."));
        list.add(new Entry("logistics", "bce_pipe.json",
                entry("BCE Pipe", item("bce_pipe"), "logistics", pipePages)));

        List<JsonObject> storagePages = new ArrayList<>();
        storagePages.addAll(textPages(null,
                "A passive BCE tank. It moves BCE through the pipes connected to it: fill it with an extract pipe from"
                        + " any BCE source, and drain it with an insert pipe into any sink. Each tier is sized to hold"
                        + " one whole band-max block of BCE."));
        storagePages.add(craftPage("BCE Storage", "A frame shell around a single interface panel.", "bce_storage"));
        storagePages.add(craftPage("BCE Storage II", "The band-1 tank, holding one full 10^15-tier block of BCE.", "bce_storage_2"));
        for (CompressionBand band : CompressionBand.values()) {
            if (band.index() < 2) {
                continue;
            }
            storagePages.add(craftPage("BCE Storage " + romanUpper(band.index() + 1),
                    "The band-" + band.index() + " tank, holding one full " + tierText(band.maxOutputTier())
                            + "-tier block of BCE.",
                    "bce_storage_" + (band.index() + 1)));
        }
        list.add(new Entry("logistics", "bce_storage.json",
                entry("BCE Storage", item("bce_storage"), "logistics", storagePages)));

        List<JsonObject> wrenchPages = new ArrayList<>();
        wrenchPages.add(spotlight("The Wrench",
                "Right-click a placed port to cycle its kind, or a pipe end to reverse that end's direction.",
                item("wrench")));
        wrenchPages.addAll(textPages("Cross-Mod",
                "The wrench is registered on the shared $(o)c:tools/wrench$() tag, so wrenches from other mods work on"
                        + " these machines and this wrench works on theirs."));
        wrenchPages.add(craftPage("Compression Wrench", "A single wrench configures every port and pipe end.",
                "wrench"));
        list.add(new Entry("logistics", "wrench.json",
                entry("Compression Wrench", item("wrench"), "logistics", wrenchPages)));

        return list;
    }

    private static JsonObject textPage(String title, String text) {
        JsonObject page = new JsonObject();
        page.addProperty("type", "patchouli:text");
        if (title != null) {
            page.addProperty("title", title);
        }
        page.addProperty("text", text);
        return page;
    }

    // Greedily packs a long body across Patchouli text pages sentence by sentence (force-wrapping an over-long
    // sentence), filling each page up to MAX_PAGE_CHARS of rendered width so no page spills a lone stub. Only the
    // first page carries the title; the continuations are untitled.
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

    // Rendered width of a page string: Patchouli macros such as $(l)/$(#00AA00) take zero on-screen space, so
    // packing must measure the text with them stripped, not the raw source length.
    private static int visibleLen(String text) {
        return MACRO.matcher(text).replaceAll("").length();
    }

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

    private enum Kind {
        COMPRESSOR,
        DECOMPRESSOR,
        GENERATOR
    }

    private static String powerPortAdvice(CompressionBand band) {
        int recommended = band.recommendedPowerPorts();
        if (recommended <= band.minPowerPorts()) {
            return "";
        }
        return " Only one Power port is $(l)required$() to form the shell, so a single-port build works; this band"
                + " $(l)recommends " + recommended + " Power ports$() so FE arrives fast enough to keep the machine at"
                + " full speed.";
    }

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
                symmetrical = false;
                coreId = block(coreBlockFor(band, kind));
                coreLayerFromTop = height - 1 - band.generatorLayersBelow();
            }
            default -> throw new IllegalStateException(kind.name());
        }

        int cx = width / 2;
        int cy = height / 2;
        boolean takesItemIn = kind != Kind.GENERATOR;

        JsonArray pattern = new JsonArray();
        for (int iy = 0; iy < height; iy++) {
            JsonArray layer = new JsonArray();
            for (int iz = 0; iz < width; iz++) {
                StringBuilder row = new StringBuilder(width);
                for (int ix = 0; ix < width; ix++) {
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

    // Multiblock preview cell codes: '0' core, 'P' power port (top centre), 'R' item-output, 'I' interface (front),
    // 'Q' item-input (left, compressors/decompressors only), 'F' frame edge, 'G' glass face, ' ' hollow interior.
    private static char cellChar(int ix, int iy, int iz, int cx, int cy, int width, int height, int coreLayerFromTop,
            boolean takesItemIn) {
        if (ix == cx && iz == cx && iy == coreLayerFromTop) {
            return '0';
        }
        if (iy == 0 && ix == cx && iz == cx) {
            return 'P';
        }
        if (iy == cy && iz == 0 && ix == cx) {
            return 'I';
        }
        if (takesItemIn && iy == cy && ix == 0 && iz == cx) {
            return 'Q';
        }
        if (iy == cy && ix == width - 1 && iz == cx) {
            return 'R';
        }
        int boundary = 0;
        if (ix == 0 || ix == width - 1) boundary++;
        if (iz == 0 || iz == width - 1) boundary++;
        if (iy == 0 || iy == height - 1) boundary++;
        if (boundary >= 2) return 'F';
        if (boundary == 1) return 'G';
        return ' ';
    }

    private static net.minecraft.world.level.block.Block coreBlockFor(CompressionBand band, Kind kind) {
        return switch (kind) {
            case COMPRESSOR -> band.index() <= 1 ? ModBlocks.CORE.get()
                    : band == CompressionBand.BAND_2 ? ModBlocks.COMPRESSION_CORE_2.get()
                    : ModBlocks.COMPRESSION_CORES_BY_BAND.get(band).get();
            case DECOMPRESSOR -> band.index() <= 1 ? ModBlocks.COMPRESSION_DECOMPRESSOR_CORE.get()
                    : band == CompressionBand.BAND_2 ? ModBlocks.COMPRESSION_DECOMPRESSOR_CORE_2.get()
                    : ModBlocks.COMPRESSION_DECOMPRESSOR_CORE_3.get();
            case GENERATOR -> band.index() <= 1 ? ModBlocks.GENERATOR_CORE.get()
                    : band == CompressionBand.BAND_2 ? ModBlocks.GENERATOR_CORE_2.get()
                    : ModBlocks.GENERATOR_CORES_BY_BAND.get(band).get();
        };
    }

    private JsonObject mapping(String coreId) {
        JsonObject map = new JsonObject();
        map.addProperty("F", block(ModBlocks.FRAME.get()));
        map.addProperty("G", block(ModBlocks.GLASS.get()));
        map.addProperty("I", blockState(ModBlocks.INTERFACE.get(), Direction.NORTH));
        map.addProperty("P", blockState(ModBlocks.PORT.get(), Direction.UP, "port_type=power"));
        map.addProperty("Q", blockState(ModBlocks.PORT.get(), Direction.WEST, "port_type=item_in"));
        map.addProperty("R", blockState(ModBlocks.PORT.get(), Direction.EAST, "port_type=item_out"));
        map.addProperty("0", coreId);
        return map;
    }

    private static String block(net.minecraft.world.level.block.Block block) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    private static String blockState(net.minecraft.world.level.block.Block block, Direction facing, String... extra) {
        StringBuilder state = new StringBuilder(block(block)).append('[');
        state.append("facing=").append(facing.getSerializedName());
        for (String pair : extra) {
            state.append(',').append(pair);
        }
        return state.append(']').toString();
    }
}
