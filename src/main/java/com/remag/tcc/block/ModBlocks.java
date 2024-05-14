package com.remag.tcc.block;

import com.remag.tcc.TCC;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, TCC.MODID);

    public static final RegistryObject<Block> COBBLESTONE_ONE = BLOCKS.register("cobblestone_one",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.COBBLESTONE)
                    .strength(2f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> COBBLESTONE_TWO = BLOCKS.register("cobblestone_two",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.COBBLESTONE)
                    .strength(2f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> COBBLESTONE_THREE = BLOCKS.register("cobblestone_three",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.COBBLESTONE)
                    .strength(2f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> COBBLESTONE_FOUR = BLOCKS.register("cobblestone_four",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.COBBLESTONE)
                    .strength(2f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> COBBLESTONE_FIVE = BLOCKS.register("cobblestone_five",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.COBBLESTONE)
                    .strength(2f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> COBBLESTONE_SIX = BLOCKS.register("cobblestone_six",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.COBBLESTONE)
                    .strength(2f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> COBBLESTONE_SEVEN = BLOCKS.register("cobblestone_seven",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.COBBLESTONE)
                    .strength(2f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> COBBLESTONE_EIGHT = BLOCKS.register("cobblestone_eight",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.COBBLESTONE)
                    .strength(2f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> COBBLESTONE_NINE = BLOCKS.register("cobblestone_nine",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.COBBLESTONE)
                    .strength(2f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> COBBLESTONE_TEN = BLOCKS.register("cobblestone_ten",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.COBBLESTONE)
                    .strength(2f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> COBBLESTONE_ELEVEN = BLOCKS.register("cobblestone_eleven",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.COBBLESTONE)
                    .strength(2f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> COBBLESTONE_TWELVE = BLOCKS.register("cobblestone_twelve",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.COBBLESTONE)
                    .strength(2f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> COBBLESTONE_THIRTEEN = BLOCKS.register("cobblestone_thirteen",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.COBBLESTONE)
                    .strength(2f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> COBBLESTONE_FOURTEEN = BLOCKS.register("cobblestone_fourteen",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.COBBLESTONE)
                    .strength(2f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> COBBLESTONE_FIFTEEN = BLOCKS.register("cobblestone_fifteen",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.COBBLESTONE)
                    .strength(2f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> COBBLESTONE_SIXTEEN = BLOCKS.register("cobblestone_sixteen",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.COBBLESTONE)
                    .strength(2f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> COBBLESTONE_SEVENTEEN = BLOCKS.register("cobblestone_seventeen",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.COBBLESTONE)
                    .strength(2f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> COBBLESTONE_EIGHTEEN = BLOCKS.register("cobblestone_eighteen",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.COBBLESTONE)
                    .strength(2f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> COBBLESTONE_NINETEEN = BLOCKS.register("cobblestone_nineteen",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.COBBLESTONE)
                    .strength(2f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> COBBLESTONE_TWENTY = BLOCKS.register("cobblestone_twenty",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.COBBLESTONE)
                    .strength(2f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> COBBLESTONE_TWENTYONE = BLOCKS.register("cobblestone_twentyone",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.COBBLESTONE)
                    .strength(2f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> COBBLESTONE_TWENTYTWO = BLOCKS.register("cobblestone_twentytwo",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.COBBLESTONE)
                    .strength(2f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> COBBLESTONE_TWENTYTHREE = BLOCKS.register("cobblestone_twentythree",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.COBBLESTONE)
                    .strength(2f).requiresCorrectToolForDrops()));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
