package net.skds.wpo.mixin.world;

import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import org.spongepowered.asm.mixin.Mixin;

import javax.annotation.Nullable;

@Mixin(EmptyChunk.class)
public class EmptyChunkMixin extends Chunk implements IChunkMixinInterface {
    public EmptyChunkMixin(World pLevel, ChunkPos pPos, BiomeContainer pBiomes) {
        super(pLevel, pPos, pBiomes);
    }

    @Nullable
    @Override
    public FluidState setFluidState(BlockPos pPos, FluidState pFluidState, boolean pIsMoving) {
        return null;
    }
}
