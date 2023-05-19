package net.skds.wpo.fluidphysics;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.skds.wpo.WPOConfig;
import net.skds.wpo.fluidphysics.FFluidStatic.FluidDisplacer2;
import net.skds.wpo.registry.BlockStateProps;
import net.skds.wpo.util.interfaces.IBaseWL;

import static net.minecraft.state.properties.BlockStateProperties.WATERLOGGED;
import static net.skds.wpo.registry.BlockStateProps.FFLUID_LEVEL;

public class TurboDisplacer {


	public static void markForDisplace(ServerWorld w, BlockPos pos, BlockState oldState, BlockState newState) {
		//World w = (World) w;
		//BlockPos pos = e.getPos();
		//BlockState oldState = w.getBlockState(pos);
		FluidState oldFS = oldState.getFluidState();
		//FluidState nfs = newState.getFluidState();
		Fluid oldFluid = oldFS.getType();
		//BlockState newState = e.getPlacedBlock();
		Block newBlock = newState.getBlock();
		int oldLvl = oldFS.getAmount();
		//int nlevel = nfs.getLevel();
		if (oldFS.isEmpty()) {
			return;
		}
		if (newState.hasProperty(WATERLOGGED)) {
			w.setBlock(pos, FFluidStatic.getUpdatedState(newState, oldLvl, oldFluid), 3);
			return;
		}

		FluidDisplacer2 displacer = new FluidDisplacer2(w, oldState);
		FFluidStatic.iterateFluidWay(10, pos, displacer);
	}
    
}
