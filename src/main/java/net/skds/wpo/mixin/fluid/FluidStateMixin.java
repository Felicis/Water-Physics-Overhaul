package net.skds.wpo.mixin.fluid;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.state.Property;
import net.minecraft.state.StateHolder;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.common.extensions.IForgeFluidState;
import net.skds.wpo.mixininterfaces.FluidMixinInterface;
import net.skds.wpo.mixininterfaces.FluidStateMixinInterface;
import net.skds.wpo.mixininterfaces.IWorldWriterMixinInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FluidState.class)
public abstract class FluidStateMixin extends StateHolder<Fluid, FluidState> implements FluidStateMixinInterface, IForgeFluidState {
    // from AbstractBlock
    private static final Direction[] UPDATE_SHAPE_ORDER = new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.DOWN, Direction.UP};

    @Shadow public abstract Fluid getType();

    protected FluidStateMixin(Fluid pOwner, ImmutableMap<Property<?>, Comparable<?>> pValues, MapCodec<FluidState> pPropertiesCodec) {
        super(pOwner, pValues, pPropertiesCodec);
    }

    @Override
    public boolean is(Fluid pTag) {
        return this.getType().isSame(pTag);
    }

    @Override
    public void onPlace(World pLevel, BlockPos pPos, FluidState pOldState, boolean pIsMoving) {
        ((FluidMixinInterface) this.getType()).onPlace(this.getFluidState(), pLevel, pPos, pOldState, pIsMoving);
    }

    @Override
    public void onRemove(World pLevel, BlockPos pPos, FluidState pNewState, boolean pIsMoving) {
        ((FluidMixinInterface) this.getType()).onRemove(this.getFluidState(), pLevel, pPos, pNewState, pIsMoving);
    }

    @Override
    public void neighborChanged(World pLevel, BlockPos pPos, Fluid pFluid, BlockPos pFromPos, boolean pIsMoving) {
        ((FluidMixinInterface) this.getType()).neighborChanged(this.getFluidState(), pLevel, pPos, pFluid, pFromPos, pIsMoving);
    }

    @Override
    public final void updateNeighbourShapes(IWorld pLevel, BlockPos pPos, int pFlag, int pRecursionLeft) {
        // from AbstractBlockState.updateNeighbourShapes
        BlockPos.Mutable blockpos$mutable = new BlockPos.Mutable();

        for(Direction direction : UPDATE_SHAPE_ORDER) {
            blockpos$mutable.setWithOffset(pPos, direction);
            FluidState neighborFluidState = pLevel.getFluidState(blockpos$mutable);
            FluidState neighborFluidStateUpdated = ((FluidStateMixinInterface)(Object) neighborFluidState).updateShape(
                    direction.getOpposite(), this.getFluidState(), pLevel, blockpos$mutable, pPos);
            ((IWorldWriterMixinInterface) pLevel).setFluid(blockpos$mutable, neighborFluidStateUpdated, pFlag, pRecursionLeft);
            // TODO setFluid or setBlockAndFluid?
        }
    }

    /**
     * Update the provided state given the provided neighbor facing and neighbor state, returning a new state.
     * For example, fences make their connections to the passed in state if possible, and wet concrete powder immediately
     * returns its solidified counterpart.
     * Note that this method should ideally consider only the specific face passed in.
     */
    @Override
    public FluidState updateShape(Direction pDirection, FluidState pQueried, IWorld pLevel, BlockPos pCurrentPos, BlockPos pOffsetPos) {
        return ((FluidMixinInterface) this.getType()).updateShape(this.getFluidState(), pDirection, pQueried, pLevel, pCurrentPos, pOffsetPos);
    }

    @Override
    public void updateIndirectNeighbourShapes(IWorld pLevel, BlockPos pPos, int pFlags, int pRecursionLeft) {
        ((FluidMixinInterface) this.getType()).updateIndirectNeighbourShapes(this.getFluidState(), pLevel, pPos, pFlags, pRecursionLeft);
    }

    @Override
    public boolean skipRendering(FluidState pAdjacentState, Direction pSide) {
        return pAdjacentState.getType().isSame(this.getType());
    }
}
