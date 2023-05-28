package net.skds.wpo.fluidphysics.actioniterables;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Set;

public interface IFluidActionIteratable {
    default void addZero(Set<BlockPos> set, BlockPos p0) {
        set.add(p0);
    }

    boolean isComplete();

    void run(BlockPos pos, BlockState state);

    World getWorld();

    boolean isValidState(BlockState state);

    default boolean isValidPos(BlockPos pos) {
        return true;
    }

    void finish();

    default void fail() {
    }
}
