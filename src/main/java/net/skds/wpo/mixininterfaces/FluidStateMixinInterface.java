package net.skds.wpo.mixininterfaces;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

public interface FluidStateMixinInterface {
    boolean is(Fluid pTag);
    void onPlace(World pLevel, BlockPos pPos, FluidState pOldState, boolean pIsMoving);

    void onRemove(World pLevel, BlockPos pPos, FluidState pNewState, boolean pIsMoving);

    void neighborChanged(World pLevel, BlockPos pPos, Fluid pFluid, BlockPos pFromPos, boolean pIsMoving);

    void updateNeighbourShapes(IWorld pLevel, BlockPos pPos, int pFlag, int pRecursionLeft);

    void updateIndirectNeighbourShapes(IWorld pLevel, BlockPos pPos, int pFlag, int pRecursionLeft);

    FluidState updateShape(Direction pDirection, FluidState pQueried, IWorld pLevel, BlockPos pCurrentPos, BlockPos pOffsetPos);

    boolean skipRendering(FluidState pAdjacentState, Direction pSide);
}
