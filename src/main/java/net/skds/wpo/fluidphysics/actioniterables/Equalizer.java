package net.skds.wpo.fluidphysics.actioniterables;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class Equalizer extends AbstractFluidActionIterable<Void> {
    @Override
    int getMaxRange() {
        return 0;
    }

    @Override
    boolean isComplete() {
        return false;
    }

    @Override
    World getWorld() {
        return null;
    }

    @Override
    boolean isValidPos(BlockPos pos) {
        return false;
    }

    @Override
    void process(BlockPos pos) {

    }

    @Override
    Void finishSuccess(int flags, int recursion) {
        return null;
    }

    @Override
    Void finishFail(int flags, int recursion) {
        return null;
    }
}
