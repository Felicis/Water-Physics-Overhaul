package net.skds.wpo.mixin.world;

import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.storage.ISpawnWorldInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Supplier;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin extends World implements ClientWorldMixinInterface, WorldMixinInterface, IWorldWriterMixinInterface {
    @Shadow
    @Final
    private WorldRenderer levelRenderer;

    protected ClientWorldMixin(ISpawnWorldInfo pLevelData, RegistryKey<World> pDimension, DimensionType pDimensionType, Supplier<IProfiler> pProfiler, boolean pIsClientSide, boolean pIsDebug, long pBiomeZoomSeed) {
        super(pLevelData, pDimension, pDimensionType, pProfiler, pIsClientSide, pIsDebug, pBiomeZoomSeed);
    }

    @Override
    public boolean containsAnyLiquid(AxisAlignedBB pBb) {
        return ((IWorldReaderMixinInterface) this).containsAnyLiquidM(pBb);
    }

    /* different for ordinals: this position and below */
    @Redirect(method = "trySpawnDripParticles",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;getFluidState()Lnet/minecraft/fluid/FluidState;", ordinal = 0))
    private FluidState getFluidState1(BlockState instance, BlockPos pBlockPos) { // one outer method argument added
        return this.getFluidState(pBlockPos);
    }

    @Redirect(method = "trySpawnDripParticles",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;getFluidState()Lnet/minecraft/fluid/FluidState;", ordinal = 1))
    private FluidState getFluidState2(BlockState instance, BlockPos pBlockPos) { // one outer method argument added
        return this.getFluidState(pBlockPos.below());
    }

    @Override
    public void setKnownState(BlockPos pPos, FluidState pState) {
        // UPGRADE: this is called for client sync => new SChangeFluidPacket and SMultiFluidChangePacket + ClientWPONetHandler
        this.setFluid(pPos, pState, 19); // 19 => no neighbor reactions & block update
    }

    @Override
    public void sendFluidUpdated(BlockPos pPos, FluidState pOldState, FluidState pNewState, int pFlags) {
        // this is okay (using blockChanged with null arguments), since the states are unused: method only sets section (pos) dirty
        this.levelRenderer.blockChanged(this, pPos, null, null, pFlags);
    }
    // UPGRADE: sendBlockUpdated does not need mixin, because this.levelRenderer.blockChanged (WorldRenderer) does not use old & new block state

    @Override
    public void setFluidsDirty(BlockPos pBlockPos, FluidState pOldState, FluidState pNewState) {
        // UPGRADE: like WorldRenderer.setBlockDirty (ClientWorld.setBlocksDirty only passes arguments)
        if (pOldState != pNewState) {
            int x = pBlockPos.getX();
            int y = pBlockPos.getY();
            int z = pBlockPos.getZ();
            this.levelRenderer.setBlocksDirty(x, y, z, x, y, z);
        }
    }
}
