package net.skds.wpo.mixin.world;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;

public interface ClientWorldMixinInterface extends IWorldWriterMixinInterface {
    void setKnownState(BlockPos pPos, FluidState pState);
}
