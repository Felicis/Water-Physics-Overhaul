package net.skds.wpo.registry;

import com.google.common.collect.ImmutableSet;
import net.minecraft.block.*;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.skds.wpo.fluidphysics.FFluidStatic;
import net.skds.wpo.util.Constants;
import net.skds.wpo.util.marker.WPOFluidMarker;
import net.skds.wpo.util.marker.WPOFluidloggableMarker;
import net.skds.wpo.util.property.RegistryEntryProperty;
import virtuoel.statement.api.StateRefresher;
import virtuoel.statement.util.RegistryUtils;

import static net.minecraft.state.properties.BlockStateProperties.WATERLOGGED;

public class BlockStateProps {

	public static final IntegerProperty WPO_LEVEL = IntegerProperty.create("wpo_level", 0, Constants.MAX_FLUID_LEVEL);
	public static final RegistryEntryProperty<Fluid> WPO_FLUID = new RegistryEntryProperty<>("wpo_fluid", Registry.FLUID);
	public static final EnumProperty<Direction> ROTATION = EnumProperty.create("frotation", Direction.class);

	public static void init() {
		for (Block block : Registry.BLOCK) { // TODO too slow: Properties are ballooning => LEVEL x WPO_LEVEL x WPO_FLUID?
			// use markers to check which blocks should get properties & add properties
			// DO NOT TRY to add properties in XXXBlock constructor => it is called before this ;) => add default values here
			if (WPOFluidloggableMarker.isWPOFluidloggable(block)){
				addWPOFluidloggableProps(block);
			}
			if (WPOFluidMarker.isWPOFluid(block)){
				addWPOFluidProps(block);
			}
		}
//		// add all fluids (as options) to WPO_FLUID property
//		StateRefresher.INSTANCE.refreshBlockStates(
//				WPO_FLUID,
//				Registry.FLUID.keySet(),
//				ImmutableSet.of()
//		);
		// reorder block states
		StateRefresher.INSTANCE.reorderBlockStates();
	}

	private static void addWPOFluidloggableProps(Block block){
		// add WATERLOGGED, WPO_FLUID and WPO_LEVEL to "normal" blocks that should get leveled water physics (with any fluid)
		// this works!! (to check for fluid do not check instanceof IBaseWL/IWaterloggable, but hasProperty)
		// TODO check if spawning waterlogged => add properties
		if (!block.defaultBlockState().hasProperty(WATERLOGGED)) {
			StateRefresher.INSTANCE.addBlockProperty(block, WATERLOGGED, false);
		}
		if (!block.defaultBlockState().hasProperty(WPO_LEVEL)) {
			StateRefresher.INSTANCE.addBlockProperty(block, WPO_LEVEL, 0); // TODO fix: not applied???
		}
		if (!block.defaultBlockState().hasProperty(WPO_FLUID)) {
			StateRefresher.INSTANCE.addBlockProperty(block, WPO_FLUID, Fluids.EMPTY.getRegistryName());
		}
	}

	private static void addWPOFluidProps(Block block){
		// add WPO_FLUID and WPO_LEVEL to fluid blocks that should get leveled water physics
		// WPO_FLUID: use correct fluid
		// WPO_LEVEL: use max level (defaultstate of fluid block is full block)
		FlowingFluid fluid = ((FlowingFluidBlock) block).getFluid();
		if (!block.defaultBlockState().hasProperty(WPO_LEVEL)) {
			StateRefresher.INSTANCE.addBlockProperty(block, WPO_LEVEL, Constants.MAX_FLUID_LEVEL); // TODO fix: not applied???
		}
		if (!block.defaultBlockState().hasProperty(WPO_FLUID)) {
			StateRefresher.INSTANCE.addBlockProperty(block, WPO_FLUID, fluid.getRegistryName()); // TODO crashes for minecraft:lava?
		}
	}

	public static ResourceLocation fluid2Property(Fluid fluid){
		return RegistryUtils.getId(Registry.FLUID, fluid);
	}

	public static Fluid property2Fluid(ResourceLocation fluidResource){
		return RegistryUtils.get(Registry.FLUID, fluidResource);
	}
}