package net.skds.wpo.mixin.world;

import net.minecraft.fluid.FluidState;
import net.minecraft.world.IBlockReader;

public interface BlockReaderMixinInterface extends IBlockReader { // extend BlockReader superclasses
    void addFluidStates(FluidState[] fstates);
}
