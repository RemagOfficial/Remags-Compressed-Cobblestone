package com.remag.tcc.block;

import com.remag.tcc.TCC;
import com.remag.tcc.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, TCC.MODID);

    public static final RegistryObject<Block> COBBLESTONE_ONE = registerBlock("cobblestone_one",
            () -> new Block(BlockBehaviour.Properties.of(Material.STONE)
                    .strength(2f).requiresCorrectToolForDrops()), ModCreativeModeTab.TCC_TAB);
    public static final RegistryObject<Block> COBBLESTONE_TWO = registerBlock("cobblestone_two",
            () -> new Block(BlockBehaviour.Properties.of(Material.STONE)
                    .strength(2f).requiresCorrectToolForDrops()), ModCreativeModeTab.TCC_TAB);
    public static final RegistryObject<Block> COBBLESTONE_THREE = registerBlock("cobblestone_three",
            () -> new Block(BlockBehaviour.Properties.of(Material.STONE)
                    .strength(2f).requiresCorrectToolForDrops()), ModCreativeModeTab.TCC_TAB);
    public static final RegistryObject<Block> COBBLESTONE_FOUR = registerBlock("cobblestone_four",
            () -> new Block(BlockBehaviour.Properties.of(Material.STONE)
                    .strength(2f).requiresCorrectToolForDrops()), ModCreativeModeTab.TCC_TAB);
    public static final RegistryObject<Block> COBBLESTONE_FIVE = registerBlock("cobblestone_five",
            () -> new Block(BlockBehaviour.Properties.of(Material.STONE)
                    .strength(2f).requiresCorrectToolForDrops()), ModCreativeModeTab.TCC_TAB);
    public static final RegistryObject<Block> COBBLESTONE_SIX = registerBlock("cobblestone_six",
            () -> new Block(BlockBehaviour.Properties.of(Material.STONE)
                    .strength(2f).requiresCorrectToolForDrops()), ModCreativeModeTab.TCC_TAB);
    public static final RegistryObject<Block> COBBLESTONE_SEVEN = registerBlock("cobblestone_seven",
            () -> new Block(BlockBehaviour.Properties.of(Material.STONE)
                    .strength(2f).requiresCorrectToolForDrops()), ModCreativeModeTab.TCC_TAB);
    public static final RegistryObject<Block> COBBLESTONE_EIGHT = registerBlock("cobblestone_eight",
            () -> new Block(BlockBehaviour.Properties.of(Material.STONE)
                    .strength(2f).requiresCorrectToolForDrops()), ModCreativeModeTab.TCC_TAB);
    public static final RegistryObject<Block> COBBLESTONE_NINE = registerBlock("cobblestone_nine",
            () -> new Block(BlockBehaviour.Properties.of(Material.STONE)
                    .strength(2f).requiresCorrectToolForDrops()), ModCreativeModeTab.TCC_TAB);
    public static final RegistryObject<Block> COBBLESTONE_TEN = registerBlock("cobblestone_ten",
            () -> new Block(BlockBehaviour.Properties.of(Material.STONE)
                    .strength(2f).requiresCorrectToolForDrops()), ModCreativeModeTab.TCC_TAB);
    public static final RegistryObject<Block> COBBLESTONE_ELEVEN = registerBlock("cobblestone_eleven",
            () -> new Block(BlockBehaviour.Properties.of(Material.STONE)
                    .strength(2f).requiresCorrectToolForDrops()), ModCreativeModeTab.TCC_TAB);
    public static final RegistryObject<Block> COBBLESTONE_TWELVE = registerBlock("cobblestone_twelve",
            () -> new Block(BlockBehaviour.Properties.of(Material.STONE)
                    .strength(2f).requiresCorrectToolForDrops()), ModCreativeModeTab.TCC_TAB);
    public static final RegistryObject<Block> COBBLESTONE_THIRTEEN = registerBlock("cobblestone_thirteen",
            () -> new Block(BlockBehaviour.Properties.of(Material.STONE)
                    .strength(2f).requiresCorrectToolForDrops()), ModCreativeModeTab.TCC_TAB);
    public static final RegistryObject<Block> COBBLESTONE_FOURTEEN = registerBlock("cobblestone_fourteen",
            () -> new Block(BlockBehaviour.Properties.of(Material.STONE)
                    .strength(2f).requiresCorrectToolForDrops()), ModCreativeModeTab.TCC_TAB);
    public static final RegistryObject<Block> COBBLESTONE_FIFTEEN = registerBlock("cobblestone_fifteen",
            () -> new Block(BlockBehaviour.Properties.of(Material.STONE)
                    .strength(2f).requiresCorrectToolForDrops()), ModCreativeModeTab.TCC_TAB);
    public static final RegistryObject<Block> COBBLESTONE_SIXTEEN = registerBlock("cobblestone_sixteen",
            () -> new Block(BlockBehaviour.Properties.of(Material.STONE)
                    .strength(2f).requiresCorrectToolForDrops()), ModCreativeModeTab.TCC_TAB);
    public static final RegistryObject<Block> COBBLESTONE_SEVENTEEN = registerBlock("cobblestone_seventeen",
            () -> new Block(BlockBehaviour.Properties.of(Material.STONE)
                    .strength(2f).requiresCorrectToolForDrops()), ModCreativeModeTab.TCC_TAB);
    public static final RegistryObject<Block> COBBLESTONE_EIGHTEEN = registerBlock("cobblestone_eighteen",
            () -> new Block(BlockBehaviour.Properties.of(Material.STONE)
                    .strength(2f).requiresCorrectToolForDrops()), ModCreativeModeTab.TCC_TAB);
    public static final RegistryObject<Block> COBBLESTONE_NINETEEN = registerBlock("cobblestone_nineteen",
            () -> new Block(BlockBehaviour.Properties.of(Material.STONE)
                    .strength(2f).requiresCorrectToolForDrops()), ModCreativeModeTab.TCC_TAB);
    public static final RegistryObject<Block> COBBLESTONE_TWENTY = registerBlock("cobblestone_twenty",
            () -> new Block(BlockBehaviour.Properties.of(Material.STONE)
                    .strength(2f).requiresCorrectToolForDrops()), ModCreativeModeTab.TCC_TAB);
    public static final RegistryObject<Block> COBBLESTONE_TWENTYONE = registerBlock("cobblestone_twentyone",
            () -> new Block(BlockBehaviour.Properties.of(Material.STONE)
                    .strength(2f).requiresCorrectToolForDrops()), ModCreativeModeTab.TCC_TAB);
    public static final RegistryObject<Block> COBBLESTONE_TWENTYTWO = registerBlock("cobblestone_twentytwo",
            () -> new Block(BlockBehaviour.Properties.of(Material.STONE)
                    .strength(2f).requiresCorrectToolForDrops()), ModCreativeModeTab.TCC_TAB);
    public static final RegistryObject<Block> COBBLESTONE_TWENTYTHREE = registerBlock("cobblestone_twentythree",
            () -> new Block(BlockBehaviour.Properties.of(Material.STONE)
                    .strength(2f).requiresCorrectToolForDrops()), ModCreativeModeTab.TCC_TAB);


    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block, CreativeModeTab tab) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn, tab);
        return toReturn;
    }

    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block,
                                                                            CreativeModeTab tab) {
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties().tab(tab)));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
