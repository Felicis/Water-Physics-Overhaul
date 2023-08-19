package net.skds.wpo.registry;

import net.minecraft.entity.EntityType;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import static net.skds.wpo.WPO.MOD_ID;

public class WPO_Entities {
	
    public static final DeferredRegister<TileEntityType<?>> TILE_ENTITIES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, MOD_ID);
    

	public static void register() {
		IEventBus eb = FMLJavaModLoadingContext.get().getModEventBus();
		ENTITIES.register(eb);
		TILE_ENTITIES.register(eb);
	}
}