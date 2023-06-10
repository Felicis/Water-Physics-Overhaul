package net.skds.wpo.mixin.world;

import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.*;
import net.skds.wpo.mixininterfaces.ChunkSectionMixinInterface;
import net.skds.wpo.mixininterfaces.IChunkMixinInterface;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(ChunkPrimer.class)
public abstract class ChunkPrimerMixin implements IChunkMixinInterface, IChunk {
    @Shadow
    @Final
    private ChunkSection[] sections;

    @Shadow
    public abstract ChunkSection getOrCreateSection(int p_217332_1_);

    @Shadow
    @Final
    private List<BlockPos> lights;

    @Shadow
    private volatile ChunkStatus status;

    @Nullable
    @Override
    public FluidState setFluidState(BlockPos pPos, FluidState oldFluidState, boolean pIsMoving) {
        // copied from ChunkPrimer.setBlockState
        int i = pPos.getX();
        int j = pPos.getY();
        int k = pPos.getZ();
        if (j >= 0 && j < 256) {
            if (this.sections[j >> 4] == Chunk.EMPTY_SECTION && oldFluidState.isEmpty()) {
                return oldFluidState;
            } else {
                // remove 9 lines lighting
                ChunkSection chunksection = this.getOrCreateSection(j >> 4);
                FluidState newFluidState = ((ChunkSectionMixinInterface) chunksection)
                        .setFluidState(i & 15, j & 15, k & 15, oldFluidState);
                // remove 4 lines lighting
                // remove 20 lines heightmap
                return newFluidState;
            }
        } else {
            return Fluids.EMPTY.defaultFluidState();
        }
    }
}
