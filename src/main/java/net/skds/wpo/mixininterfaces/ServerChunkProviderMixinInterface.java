package net.skds.wpo.mixininterfaces;

import net.minecraft.util.math.BlockPos;

public interface ServerChunkProviderMixinInterface {
    void fluidChanged(BlockPos pPos);
}
