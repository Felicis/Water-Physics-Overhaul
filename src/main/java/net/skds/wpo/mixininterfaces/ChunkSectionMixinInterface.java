package net.skds.wpo.mixininterfaces;

import net.minecraft.fluid.FluidState;
import net.minecraft.util.palette.PalettedContainer;

public interface ChunkSectionMixinInterface {
    FluidState setFluidState(int pX, int pY, int pZ, FluidState pFluidState);

    FluidState setFluidState(int pX, int pY, int pZ, FluidState pFluidState, boolean pUseLocks);

    PalettedContainer<FluidState> getFluidStates();
}
