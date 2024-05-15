package com.remag.tcc.item;

import com.remag.tcc.TCC;
import com.remag.tcc.block.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.remag.tcc.block.ModCreativeModeTab.addToTab;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TCC.MODID);


    public static final RegistryObject<Item> COBBLESTONE_ONE = addToTab(ITEMS.register("cobblestone_one",
            () -> new BlockItem(ModBlocks.COBBLESTONE_ONE.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_TWO = addToTab(ITEMS.register("cobblestone_two",
            () -> new BlockItem(ModBlocks.COBBLESTONE_TWO.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_THREE = addToTab(ITEMS.register("cobblestone_three",
            () -> new BlockItem(ModBlocks.COBBLESTONE_THREE.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_FOUR = addToTab(ITEMS.register("cobblestone_four",
            () -> new BlockItem(ModBlocks.COBBLESTONE_FOUR.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_FIVE = addToTab(ITEMS.register("cobblestone_five",
            () -> new BlockItem(ModBlocks.COBBLESTONE_FIVE.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_SIX = addToTab(ITEMS.register("cobblestone_six",
            () -> new BlockItem(ModBlocks.COBBLESTONE_SIX.get(),
                    new Item.Properties()

            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_SEVEN = addToTab(ITEMS.register("cobblestone_seven",
            () -> new BlockItem(ModBlocks.COBBLESTONE_SEVEN.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_EIGHT = addToTab(ITEMS.register("cobblestone_eight",
            () -> new BlockItem(ModBlocks.COBBLESTONE_EIGHT.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_NINE = addToTab(ITEMS.register("cobblestone_nine",
            () -> new BlockItem(ModBlocks.COBBLESTONE_NINE.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_TEN = addToTab(ITEMS.register("cobblestone_ten",
            () -> new BlockItem(ModBlocks.COBBLESTONE_TEN.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_ELEVEN = addToTab(ITEMS.register("cobblestone_eleven",
            () -> new BlockItem(ModBlocks.COBBLESTONE_ELEVEN.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_TWELVE = addToTab(ITEMS.register("cobblestone_twelve",
            () -> new BlockItem(ModBlocks.COBBLESTONE_TWELVE.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_THIRTEEN = addToTab(ITEMS.register("cobblestone_thirteen",
            () -> new BlockItem(ModBlocks.COBBLESTONE_THIRTEEN.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_FOURTEEN = addToTab(ITEMS.register("cobblestone_fourteen",
            () -> new BlockItem(ModBlocks.COBBLESTONE_FOURTEEN.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_FIFTEEN = addToTab(ITEMS.register("cobblestone_fifteen",
            () -> new BlockItem(ModBlocks.COBBLESTONE_FIFTEEN.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_SIXTEEN = addToTab(ITEMS.register("cobblestone_sixteen",
            () -> new BlockItem(ModBlocks.COBBLESTONE_SIXTEEN.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_SEVENTEEN = addToTab(ITEMS.register("cobblestone_seventeen",
            () -> new BlockItem(ModBlocks.COBBLESTONE_SEVENTEEN.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_EIGHTEEN = addToTab(ITEMS.register("cobblestone_eighteen",
            () -> new BlockItem(ModBlocks.COBBLESTONE_EIGHTEEN.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_NINETEEN = addToTab(ITEMS.register("cobblestone_nineteen",
            () -> new BlockItem(ModBlocks.COBBLESTONE_NINETEEN.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_TWENTY = addToTab(ITEMS.register("cobblestone_twenty",
            () -> new BlockItem(ModBlocks.COBBLESTONE_TWENTY.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_TWENTYONE = addToTab(ITEMS.register("cobblestone_twentyone",
            () -> new BlockItem(ModBlocks.COBBLESTONE_TWENTYONE.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_TWENTYTWO = addToTab(ITEMS.register("cobblestone_twentytwo",
            () -> new BlockItem(ModBlocks.COBBLESTONE_TWENTYTWO.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_TWENTYTHREE = addToTab(ITEMS.register("cobblestone_twentythree",
            () -> new BlockItem(ModBlocks.COBBLESTONE_TWENTYTHREE.get(),
                    new Item.Properties()
            )
    ));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
