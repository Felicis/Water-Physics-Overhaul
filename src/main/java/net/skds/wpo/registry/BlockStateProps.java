package net.skds.wpo.registry;

import net.minecraft.block.*;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.util.Direction;
import net.minecraft.util.registry.Registry;
import net.skds.wpo.WPOConfig;
import virtuoel.statement.api.StateRefresher;

import static net.minecraft.state.properties.BlockStateProperties.WATERLOGGED;

public class BlockStateProps {

	public static final IntegerProperty FFLUID_LEVEL = IntegerProperty.create("ffluid_level", 0, WPOConfig.MAX_FLUID_LEVEL);
	public static final EnumProperty<Direction> ROTATION = EnumProperty.create("frotation", Direction.class);

	public static void init() {
		for (Block block : Registry.BLOCK) {
//			addFluidProps(block); // does NOT work => waterlogs also full blocks!
			if (block instanceof LecternBlock)
				addFluidProps(block);
			if (block instanceof FenceBlock) // vs glass pane => ok (fixed glitch of vanilla waterloggable blocks)
				addFluidProps(block);
			if (block instanceof RedstoneTorchBlock) // normal torch => works (decide which blocks should break & dont add them!)
				addFluidProps(block);
			if (block instanceof RailBlock) // vs DetectorRail (wall, rail, NOT in 1.16=lightning rod) => works
				addFluidProps(block);
		}
		StateRefresher.INSTANCE.reorderBlockStates();
	}

	private static void addFluidProps(Block block){
		// this works!! (to check for fluid do not check instanceof IBaseWL/IWaterloggable, but hasProperty)
		StateRefresher.INSTANCE.addBlockProperty(block, FFLUID_LEVEL, 0);
		StateRefresher.INSTANCE.addBlockProperty(block, WATERLOGGED, false);
		// FALLING does not seem to be needed...
	}
}