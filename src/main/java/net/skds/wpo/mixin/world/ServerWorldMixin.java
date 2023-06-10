package net.skds.wpo.mixin.world;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.ISpawnWorldInfo;
import net.minecraftforge.common.extensions.IForgeWorldServer;
import net.skds.wpo.mixininterfaces.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Supplier;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World implements WorldMixinInterface, IWorldMixinInterface, IWorldWriterMixinInterface, ISeedReader, IForgeWorldServer {
    @Shadow
    public abstract ServerChunkProvider getChunkSource();

    protected ServerWorldMixin(ISpawnWorldInfo pLevelData, RegistryKey<World> pDimension, DimensionType pDimensionType, Supplier<IProfiler> pProfiler, boolean pIsClientSide, boolean pIsDebug, long pBiomeZoomSeed) {
        super(pLevelData, pDimension, pDimensionType, pProfiler, pIsClientSide, pIsDebug, pBiomeZoomSeed);
    }

    @Override
    public boolean containsAnyLiquid(AxisAlignedBB pBb) {
        return ((IWorldReaderMixinInterface) this).containsAnyLiquidM(pBb);
    }

    // this is a double mixin to access a getFluidState deep inside a for loop without having to roadblock inject/override the entire method
    // the parameters after pZ are from tickChunk (needed to get the offsets)
    @Redirect(method = "tickChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/ChunkSection;getBlockState(III)Lnet/minecraft/block/BlockState;"))
    private BlockState getBlockStateAndTickFluidState(ChunkSection chunkSection, int pX, int pY, int pZ, Chunk pChunk, int pRandomTickSpeed) {
        // added: compute fluid ticks here, where we can access the random block position and the chunksection
        ChunkPos chunkpos = pChunk.getPos();
        int i = chunkpos.getMinBlockX();
        int j = chunkpos.getMinBlockZ();
        int k = chunkSection.bottomBlockY();
        FluidState fluidState = chunkSection.getFluidState(pX, pY, pZ);
        BlockPos blockPos = new BlockPos(pX + i, pY + k, pZ + j);
        if (fluidState.isRandomlyTicking()) {
            fluidState.randomTick(this, blockPos, this.random);
        }
        // original method behaviour
        return chunkSection.getBlockState(pX, pY, pZ);
    }

    @Redirect(method = "tickChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;getFluidState()Lnet/minecraft/fluid/FluidState;"))
    private FluidState getFluidStateNull(BlockState instance) {
        return null; // NOP: BlockState.getFluidState() should not be called. Will not crash because next mixin redirects FluidState.isRandomlyTicking
    }

    @Redirect(method = "tickChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/fluid/FluidState;isRandomlyTicking()Z"))
    private boolean isRandomlyTickingFalse(FluidState instance) {
        return true; // NOP: fluidstates were already ticked in getBlockState redirect
    }

    /**
     * sends fluid state update to clients
     *
     * @param pPos
     * @param pOldState
     * @param pNewState
     * @param pFlags
     */
    public void sendFluidUpdated(BlockPos pPos, FluidState pOldState, FluidState pNewState, int pFlags) {
        // used in:
        // - World.markAndNotifyBlock()
        // - BlockSnapshot.restoreToLocation()
        // - ... block entity stuff (not used for fluid states)
        ((ServerChunkProviderMixinInterface) this.getChunkSource()).fluidChanged(pPos); // cast
        // no pathfinding update needed
    }

    /**
     * notifies neighbors that fluid state changed (after setFluid)
     *
     * @param pPos
     * @param pFluid
     */
    @Override
    public void fluidUpdated(BlockPos pPos, Fluid pFluid) {
        if (!this.isDebug()) {
            this.updateNeighborsAt(pPos, pFluid); // for neighbor: World.neighborChanged() -> (BlockState) world.getBlockState(pos).neighborChanged(...)
        }
    }

    // INFO: skip onFluidStateChange (= onBlockStateChange): not needed bc only update POIs which (afaik) only depend on BlockStates
}
