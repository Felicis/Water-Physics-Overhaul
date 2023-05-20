package net.skds.wpo.util.api;

import net.minecraft.block.BlockState;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.skds.wpo.fluidphysics.FFluidStatic;

import javax.swing.text.html.BlockView;

public interface FluidLoggable extends IWaterLoggable
{
    @Override
    default boolean canPlaceLiquid(IBlockReader pLevel, BlockPos pPos, BlockState pState, Fluid pFluid) {
        return IWaterLoggable.super.canPlaceLiquid(pLevel, pPos, pState, pFluid);
    }

    @Override
    default boolean placeLiquid(IWorld pLevel, BlockPos pPos, BlockState pState, FluidState pFluidState) {
        return IWaterLoggable.super.placeLiquid(pLevel, pPos, pState, pFluidState);
    }

    @Override
    default Fluid takeLiquid(IWorld p_204508_1_, BlockPos p_204508_2_, BlockState p_204508_3_) {
        return IWaterLoggable.super.takeLiquid(p_204508_1_, p_204508_2_, p_204508_3_);
    }
}

