package net.skds.wpo.mixin.fluid;

import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.network.DebugPacketSender;
import net.minecraft.util.Direction;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.common.extensions.IForgeFluid;
import net.minecraftforge.registries.ForgeRegistryEntry;
import net.skds.wpo.mixininterfaces.FluidMixinInterface;
import net.skds.wpo.util.marker.WPOFluidMarker;
import org.spongepowered.asm.mixin.Mixin;

import javax.annotation.Nullable;

@Mixin(Fluid.class)
public abstract class FluidMixin extends ForgeRegistryEntry<Fluid> implements WPOFluidMarker, FluidMixinInterface, IForgeFluid {
    /**************+ MARKER MIXIN: do not remove this mixing even if empty ****************/

    @Nullable
    private String descriptionId; // copied from Block

    @Override
    public void onPlace(FluidState pState, World pLevel, BlockPos pPos, FluidState pOldState, boolean pIsMoving) {
        // TODO schedule fluid ticks?
    }

    @Override
    public void onRemove(FluidState pState, World pLevel, BlockPos pPos, FluidState pNewState, boolean pIsMoving) {
        // do nothing? (Block was removing block entity)
        // update BlockSTate.waterlogged?
    }

    @Override
    public void neighborChanged(FluidState pState, World pLevel, BlockPos pPos, Fluid pFluid, BlockPos pFromPos, boolean pIsMoving) {
        // copied from AbstractBlock.neighborChanged()
        DebugPacketSender.sendNeighborsUpdatePacket(pLevel, pPos); // TODO Override in Fluids
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

    @Override
    public FluidState updateShape(FluidState pState, Direction pFacing, FluidState pFacingState, IWorld pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
        // TODO: tick fluids here (override in
        //  also: remove fluid ticking (and WATERLOGGED check) from AbstractBlockState.updateShape() ???
        return pState;
    }

    @Override
    public void updateIndirectNeighbourShapes(FluidState pState, IWorld pLevel, BlockPos pPos, int pFlags, int pRecursionLeft) {
        // OVERRIDE (for Block only overridden in RedstoneWireBlock)
    }
}
