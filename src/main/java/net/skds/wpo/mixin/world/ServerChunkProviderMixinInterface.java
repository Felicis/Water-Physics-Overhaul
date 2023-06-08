package net.skds.wpo.mixin.world;

import net.minecraft.util.math.BlockPos;

public interface ServerChunkProviderMixinInterface {
    void fluidChanged(BlockPos pPos);
}
