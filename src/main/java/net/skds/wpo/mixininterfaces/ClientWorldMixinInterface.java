package net.skds.wpo.mixininterfaces;

import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.skds.wpo.mixininterfaces.IWorldWriterMixinInterface;

public interface ClientWorldMixinInterface extends IWorldWriterMixinInterface {
    void setKnownState(BlockPos pPos, FluidState pState);
}
