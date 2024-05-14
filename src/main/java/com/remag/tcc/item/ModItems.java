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
    public static final RegistryObject<Item> COBBLESTONE_ONE_ITEM = addToTab(ITEMS.register("cobblestone_one_item",
            () -> new BlockItem(ModBlocks.COBBLESTONE_ONE.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_TWO_ITEM = addToTab(ITEMS.register("cobblestone_two_item",
            () -> new BlockItem(ModBlocks.COBBLESTONE_TWO.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_THREE_ITEM = addToTab(ITEMS.register("cobblestone_three_item",
            () -> new BlockItem(ModBlocks.COBBLESTONE_THREE.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_FOUR_ITEM = addToTab(ITEMS.register("cobblestone_four_item",
            () -> new BlockItem(ModBlocks.COBBLESTONE_FOUR.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_FIVE_ITEM = addToTab(ITEMS.register("cobblestone_five_item",
            () -> new BlockItem(ModBlocks.COBBLESTONE_FIVE.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_SIX_ITEM = addToTab(ITEMS.register("cobblestone_six_item",
            () -> new BlockItem(ModBlocks.COBBLESTONE_SIX.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_SEVEN_ITEM = addToTab(ITEMS.register("cobblestone_seven_item",
            () -> new BlockItem(ModBlocks.COBBLESTONE_SEVEN.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_EIGHT_ITEM = addToTab(ITEMS.register("cobblestone_eight_item",
            () -> new BlockItem(ModBlocks.COBBLESTONE_EIGHT.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_NINE_ITEM = addToTab(ITEMS.register("cobblestone_nine_item",
            () -> new BlockItem(ModBlocks.COBBLESTONE_NINE.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_TEN_ITEM = addToTab(ITEMS.register("cobblestone_ten_item",
            () -> new BlockItem(ModBlocks.COBBLESTONE_TEN.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_ELEVEN_ITEM = addToTab(ITEMS.register("cobblestone_eleven_item",
            () -> new BlockItem(ModBlocks.COBBLESTONE_ELEVEN.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_TWELVE_ITEM = addToTab(ITEMS.register("cobblestone_twelve_item",
            () -> new BlockItem(ModBlocks.COBBLESTONE_TWELVE.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_THIRTEEN_ITEM = addToTab(ITEMS.register("cobblestone_thirteen_item",
            () -> new BlockItem(ModBlocks.COBBLESTONE_THIRTEEN.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_FOURTEEN_ITEM = addToTab(ITEMS.register("cobblestone_fourteen_item",
            () -> new BlockItem(ModBlocks.COBBLESTONE_FOURTEEN.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_FIFTEEN_ITEM = addToTab(ITEMS.register("cobblestone_fifteen_item",
            () -> new BlockItem(ModBlocks.COBBLESTONE_FIFTEEN.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_SIXTEEN_ITEM = addToTab(ITEMS.register("cobblestone_sixteen_item",
            () -> new BlockItem(ModBlocks.COBBLESTONE_SIXTEEN.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_SEVENTEEN_ITEM = addToTab(ITEMS.register("cobblestone_seventeen_item",
            () -> new BlockItem(ModBlocks.COBBLESTONE_SEVENTEEN.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_EIGHTEEN_ITEM = addToTab(ITEMS.register("cobblestone_eighteen_item",
            () -> new BlockItem(ModBlocks.COBBLESTONE_EIGHTEEN.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_NINETEEN_ITEM = addToTab(ITEMS.register("cobblestone_nineteen_item",
            () -> new BlockItem(ModBlocks.COBBLESTONE_NINETEEN.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_TWENTY_ITEM = addToTab(ITEMS.register("cobblestone_twenty_item",
            () -> new BlockItem(ModBlocks.COBBLESTONE_TWENTY.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_TWENTYONE_ITEM = addToTab(ITEMS.register("cobblestone_twentyone_item",
            () -> new BlockItem(ModBlocks.COBBLESTONE_TWENTYONE.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_TWENTYTWO_ITEM = addToTab(ITEMS.register("cobblestone_twentytwo_item",
            () -> new BlockItem(ModBlocks.COBBLESTONE_TWENTYTWO.get(),
                    new Item.Properties()
            )
    ));

    public static final RegistryObject<Item> COBBLESTONE_TWENTYTHREE_ITEM = addToTab(ITEMS.register("cobblestone_twentythree_item",
            () -> new BlockItem(ModBlocks.COBBLESTONE_TWENTYTHREE.get(),
                    new Item.Properties()
            )
    ));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
