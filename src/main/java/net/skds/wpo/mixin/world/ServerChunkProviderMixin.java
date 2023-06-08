package net.skds.wpo.mixin.world;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.AbstractChunkProvider;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ServerChunkProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(ServerChunkProvider.class)
public abstract class ServerChunkProviderMixin extends AbstractChunkProvider implements ServerChunkProviderMixinInterface {
    @Shadow
    @Nullable
    protected abstract ChunkHolder getVisibleChunkIfPresent(long p_217213_1_);

    @Override
    public void fluidChanged(BlockPos pPos) {
        int i = pPos.getX() >> 4;
        int j = pPos.getZ() >> 4;
        ChunkHolder chunkholder = this.getVisibleChunkIfPresent(ChunkPos.asLong(i, j));
        if (chunkholder != null) {
            ((ChunkHolderMixinInterface) chunkholder).fluidChanged(pPos);
        }
    }
}
