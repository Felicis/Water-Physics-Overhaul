package net.skds.wpo.mixininterfaces;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.skds.wpo.fluidphysics.FluidFlowCache;

import javax.annotation.Nullable;

public interface WorldMixinInterface extends IWorldWriterMixinInterface {
    FluidFlowCache getFlowCache(); // maybe move to IWorldMixinInterface...

    boolean setBlockNoFluid(BlockPos pPos, BlockState pNewState);

    boolean setBlockNoFluid(BlockPos pPos, BlockState pNewState, int pFlags);

    boolean setBlockNoFluid(BlockPos pPos, BlockState pNewState, int pFlags, int recursion);

    /**
     * sends fluid state update to clients
     *
     * @param pPos
     * @param pOldState
     * @param pNewState
     * @param pFlags
     */
    void sendFluidUpdated(BlockPos pPos, FluidState pOldState, FluidState pNewState, int pFlags);

    void setFluidsDirty(BlockPos pBlockPos, FluidState pOldState, FluidState pNewState);

    void markAndNotifyFluid(BlockPos pPos, @Nullable Chunk chunk, FluidState actualFluidState, FluidState fluidState, int pFlags, int pRecursionLeft);

    void updateNeighborsAt(BlockPos pPos, Fluid pFluid);

    void neighborChanged(BlockPos pPos, Fluid pFluid, BlockPos pFromPos);

    boolean setFluid(BlockPos pPos, FluidState pState);

    void updateNeighborsAtExceptFromFacing(BlockPos pPos, Fluid pFluidType, Direction pSkipSide);
}
