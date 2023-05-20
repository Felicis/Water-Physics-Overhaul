package net.skds.wpo.registry;

import net.minecraft.block.*;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.util.Direction;
import net.minecraft.util.registry.Registry;
import net.skds.wpo.WPOConfig;
import net.skds.wpo.util.AutomaticFluidloggableMarker;
import virtuoel.statement.api.StateRefresher;

import static net.minecraft.state.properties.BlockStateProperties.WATERLOGGED;

public class BlockStateProps {

	public static final IntegerProperty FFLUID_LEVEL = IntegerProperty.create("ffluid_level", 0, WPOConfig.MAX_FLUID_LEVEL);
	public static final EnumProperty<Direction> ROTATION = EnumProperty.create("frotation", Direction.class);

	public static void init() {
		for (Block block : Registry.BLOCK) {
			if (AutomaticFluidloggableMarker.shouldAddProperties(block)){
				addFluidProps(block);
			}
		}
		StateRefresher.INSTANCE.reorderBlockStates();
	}

	private static void addFluidProps(Block block){
		// this works!! (to check for fluid do not check instanceof IBaseWL/IWaterloggable, but hasProperty)
		if (!block.defaultBlockState().hasProperty(WATERLOGGED)) {
			StateRefresher.INSTANCE.addBlockProperty(block, WATERLOGGED, false);
		}
		if (!block.defaultBlockState().hasProperty(FFLUID_LEVEL)) {
			StateRefresher.INSTANCE.addBlockProperty(block, FFLUID_LEVEL, 0);
		}
		// FALLING does not seem to be needed...
	}
}