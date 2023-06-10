package net.skds.wpo.mixininterfaces;

import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.skds.wpo.mixininterfaces.IWorldWriterMixinInterface;

import javax.annotation.Nullable;

public interface FluidMixinInterface {
    void onPlace(FluidState pState, World pLevel, BlockPos pPos, FluidState pOldState, boolean pIsMoving);

    void onRemove(FluidState pState, World pLevel, BlockPos pPos, FluidState pNewState, boolean pIsMoving);

    static int getId(@Nullable FluidState pState) {
        if (pState == null) {
            return 0;
        } else {
            int i = Fluid.FLUID_STATE_REGISTRY.getId(pState);
            return i == -1 ? 0 : i;
        }
    }

    void neighborChanged(FluidState pState, World pLevel, BlockPos pPos, Fluid pFluid, BlockPos pFromPos, boolean pIsMoving);

    String getDescriptionId();

    static void updateOrDestroy(FluidState pOldState, FluidState pNewState, IWorld pLevel, BlockPos pPos, int pFlags, int pRecursionLeft) {
        if (pNewState != pOldState) {
            if (pNewState.isEmpty()) {
                // fluid does not drop (destroyBlock =>
            } else {
                // -33 = 0b1101_1111 => drop: NO_NEIGHBOR_DROPS
                ((IWorldWriterMixinInterface) pLevel).setFluid(pPos, pNewState, pFlags & -33, pRecursionLeft);
            }
        }
    }

    FluidState updateShape(FluidState pState, Direction pFacing, FluidState pFacingState, IWorld pLevel, BlockPos pCurrentPos, BlockPos pFacingPos);

    void updateIndirectNeighbourShapes(FluidState pState, IWorld pLevel, BlockPos pPos, int pFlags, int pRecursionLeft);
}
