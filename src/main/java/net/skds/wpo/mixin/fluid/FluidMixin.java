package net.skds.wpo.mixin.fluid;

import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.Direction;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.common.extensions.IForgeFluid;
import net.minecraftforge.registries.ForgeRegistryEntry;
import net.skds.wpo.fluidphysics.FFluidStatic;
import net.skds.wpo.mixininterfaces.FluidMixinInterface;
import org.spongepowered.asm.mixin.Mixin;

import javax.annotation.Nullable;

@Mixin(Fluid.class)
public abstract class FluidMixin extends ForgeRegistryEntry<Fluid> implements FluidMixinInterface, IForgeFluid {
    @Nullable
    private String descriptionId; // copied from Block

    /**
     * called (on server) in Chunk.setFluidState after set if newState != oldState
     * @param pNewState
     * @param pLevel
     * @param pPos
     * @param pOldState
     * @param pIsMoving
     */
    @Override
    public void onPlace(FluidState pNewState, World pLevel, BlockPos pPos, FluidState pOldState, boolean pIsMoving) {
        // schedule fluid ticks after updating (this is called after successful setFluid, where FluidState actually changed)
        FFluidStatic.scheduleFluidTick(pLevel, pPos);
    }

    /**
     * called (on server) in Chunk.setFluidState if newState != oldState and newState was actually placed
     * @param pOldState
     * @param pLevel
     * @param pPos
     * @param pNewState
     * @param pIsMoving
     */
    @Override
    public void onRemove(FluidState pOldState, World pLevel, BlockPos pPos, FluidState pNewState, boolean pIsMoving) {
        // TODO do nothing? (Block removes block entity)
        // update BlockSTate.waterlogged?
    }

    @Override
    public void neighborChanged(FluidState pState, World pLevel, BlockPos pPos, Fluid pFluid, BlockPos pFromPos, boolean pIsMoving) {
        //
        FFluidStatic.scheduleFluidTick(pLevel, pPos);
    }

    @Override
    /**
     * Returns the unlocalized name of the fluid with "fluid." appended to the front.
     */
    public String getDescriptionId() {
        // copied from Block.getDescriptionId()
        // used for neighborChanged crash message
        if (this.descriptionId == null) {
            this.descriptionId = Util.makeDescriptionId("fluid", Registry.FLUID.getKey(this.getFluid()));
        }
        return this.descriptionId;
    }

    /**
     * Update the provided state given the provided neighbor facing and neighbor state, returning a new state.
     * For example, fences make their connections to the passed in state if possible, and wet concrete powder immediately
     * returns its solidified counterpart.
     * Note that this method should ideally consider only the specific face passed in.
     */
    @Override
    public FluidState updateShape(FluidState pState, Direction pFacing, FluidState pFacingState, IWorld pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
        // do something here?
        return pState;
    }
}
