package net.skds.wpo.mixininterfaces;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.world.IStructureReader;

public interface ChunkSectionMixinInterface {
    FluidState setFluidState(int pX, int pY, int pZ, FluidState pFluidState);

    FluidState setFluidState(int pX, int pY, int pZ, FluidState pFluidState, boolean pUseLocks);
}
