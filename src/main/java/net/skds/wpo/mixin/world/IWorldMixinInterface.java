package net.skds.wpo.mixin.world;

import net.minecraft.fluid.Fluid;
import net.minecraft.util.math.BlockPos;

public interface IWorldMixinInterface {
    default void fluidUpdated(BlockPos pPos, Fluid pFluid) {
    }
}
