package net.skds.wpo.mixin.world;

import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.palette.UpgradeData;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.ChunkPrimerWrapper;
import net.skds.wpo.mixininterfaces.IChunkMixinInterface;
import org.spongepowered.asm.mixin.Mixin;

import javax.annotation.Nullable;

@Mixin(ChunkPrimerWrapper.class)
public abstract class ChunkPrimerWrapperMixin extends ChunkPrimer implements IChunkMixinInterface {
    public ChunkPrimerWrapperMixin(ChunkPos p_i48700_1_, UpgradeData p_i48700_2_) {
        super(p_i48700_1_, p_i48700_2_);
    }

    @Nullable
    @Override
    public FluidState setFluidState(BlockPos pPos, FluidState pFluidState, boolean pIsMoving) {
        return null; // like setBlockState in ChunkPrimerWrapper
    }
}
