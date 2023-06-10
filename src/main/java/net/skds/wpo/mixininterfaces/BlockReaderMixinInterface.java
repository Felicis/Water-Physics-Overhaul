package net.skds.wpo.mixininterfaces;

import net.minecraft.fluid.FluidState;
import net.minecraft.world.IBlockReader;

public interface BlockReaderMixinInterface extends IBlockReader { // extend BlockReader superclasses
    void addFluidStates(FluidState[] fstates);
}
