package net.skds.wpo.mixininterfaces;

import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

public interface IWorldWriterMixinInterface { // IWorldWriter has no superclasses
    boolean setFluid(BlockPos pPos, FluidState pFluidState, int pFlags, int pRecursionLeft);

    /**
     * Sets a fluid state into this world.Flags are as follows: (also check {@link Constants.BlockFlags})
     * 1 will cause a block update.
     * 2 will send the change to clients.
     * 4 will prevent the block from being re-rendered.
     * 8 will force any re-renders to run on the main thread instead
     * 16 will prevent neighbor reactions (e.g. fences connecting, observers pulsing).
     * 32 will prevent neighbor reactions from spawning drops.
     * 64 will signify the block is being moved.
     * Flags can be OR-ed
     */
    default boolean setFluid(BlockPos pPos, FluidState pNewState, int pFlags) {
        // SKIPPING OVERRIDE in World: setBlock is overridden in World with same body as in IWorldWriter?!
        return this.setFluid(pPos, pNewState, pFlags, 512);
    }

    boolean removeFluid(BlockPos pPos, boolean pIsMoving);

    // do not add methods to destroy fluid, because the player should not be able to do that like with blocks
}
