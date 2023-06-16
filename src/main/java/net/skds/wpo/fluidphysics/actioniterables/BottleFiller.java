package net.skds.wpo.fluidphysics.actioniterables;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.skds.wpo.WPOConfig;
import net.skds.wpo.fluidphysics.FFluidStatic;
import net.skds.wpo.util.tuples.Tuple2;

public class BottleFiller extends AbstractFluidActionIterable<Void> {

    int bottleLevels = 3;
    int sl = 0;
    boolean complete = false;
    World world;
    FlowingFluid fluid;
    Long2ObjectLinkedOpenHashMap<FluidState> states = new Long2ObjectLinkedOpenHashMap<>();

    public BottleFiller(World w, FlowingFluid f) {
        world = w;
        fluid = f;
    }

    @Override
    protected int getMaxRange() {
        return WPOConfig.COMMON.maxBucketDist.get();
    }

    @Override
    protected boolean isComplete() {
        return complete;
    }

    @Override
    protected World getWorld() {
        return world;
    }

    @Override
    protected boolean isValidPos(BlockPos pos) {
        FluidState fluidState = world.getFluidState(pos);
        return fluid.isSame(fluidState.getType()); // implies not empty and not zero levels
    }

    @Override
    protected void process(BlockPos pos) {
        Tuple2<Integer, FluidState> takenLvlsAndNewFS = FFluidStatic.takeLevelsUpTo(world.getFluidState(pos), bottleLevels);
        Integer takenLevels = takenLvlsAndNewFS.first;
        FluidState newFluidState = takenLvlsAndNewFS.second;
        if (takenLevels > 0) { // levels were actually taken
            bottleLevels -= takenLevels;
            complete = (bottleLevels == 0);
            states.put(pos.asLong(), newFluidState);
        } // else do nothing
    }

    @Override
    protected Void finishSuccess(int flags, int recursion) {
        multiSetFluid(states, flags, recursion);
        return null;
    }

    @Override
    protected Void finishFail(int flags, int recursion) {
        return null;
    }
}
