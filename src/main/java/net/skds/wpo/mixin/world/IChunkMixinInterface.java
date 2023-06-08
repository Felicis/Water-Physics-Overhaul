package net.skds.wpo.mixin.world;

import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IStructureReader;

import javax.annotation.Nullable;

public interface IChunkMixinInterface extends IBlockReader, IStructureReader { // extends IChunk superclasses
    @Nullable
    FluidState setFluidState(BlockPos pPos, FluidState pFluidState, boolean pIsMoving);
}
