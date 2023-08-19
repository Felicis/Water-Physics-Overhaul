package net.skds.wpo.fluidphysics.flowiterators;

import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.skds.wpo.config.WPOConfig;
import net.skds.wpo.fluidphysics.FluidStatic;
import net.skds.wpo.util.tuples.Tuple2;

import java.util.HashMap;
import java.util.Map;

public class BottleFiller extends AbstractFlowIterator<Void> {

    int bottleLevels = 3;
    FlowingFluid fluid;
    Map<BlockPos, FluidState> states = new HashMap<>();

    public BottleFiller(World w, BlockPos startPos, FlowingFluid f) {
        super(w, startPos, WPOConfig.COMMON.maxBucketDist.get());
        fluid = f;
    }

    @Override
    protected boolean isValidPos(BlockPos pos) {
        FluidState fluidState = world.getFluidState(pos);
        return fluid.isSame(fluidState.getType()); // implies not empty and not zero levels
    }

    @Override
    protected void process(BlockPos pos) {
        Tuple2<Integer, FluidState> takenLvlsAndNewFS = FluidStatic.takeLevelsUpTo(world.getFluidState(pos), fluid, bottleLevels);
        Integer takenLevels = takenLvlsAndNewFS.first;
        FluidState newFluidState = takenLvlsAndNewFS.second;
        if (takenLevels > 0) { // levels were actually taken
            bottleLevels -= takenLevels;
            isComplete = (bottleLevels == 0);
            states.put(pos, newFluidState);
        } // else do nothing
    }

    @Override
    protected Void finishComplete(int flags, int recursion) {
        multiSetFluid(states, flags, recursion);
        return null;
    }

    @Override
    protected Void finishNotComplete(int flags, int recursion) {
        return null;
    }
}
