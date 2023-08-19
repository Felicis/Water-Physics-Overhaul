package net.skds.wpo.registry;

import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.skds.wpo.WPO;
import net.skds.wpo.item.AdvancedBucket;

public class WPO_Items {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, WPO.MOD_ID);
    
	public static final RegistryObject<Item> ADVANCED_BUCKET = ITEMS.register("advanced_bucket", () -> AdvancedBucket.getBucketForReg(Fluids.EMPTY));

	public static void register() {
		ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
	}
}