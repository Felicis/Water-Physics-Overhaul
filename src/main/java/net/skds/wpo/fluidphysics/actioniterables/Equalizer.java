package net.skds.wpo.fluidphysics.actioniterables;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.skds.wpo.WPOConfig;
import net.skds.wpo.fluidphysics.FFluidStatic;
import net.skds.wpo.util.tuples.Tuple3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Equalizer extends AbstractFluidActionIterable<Void> {
    private final FlowingFluid fluid;
    int levelsAtStartPos;
    Map<BlockPos, FluidState> states = new HashMap<>();

    public Equalizer(World world, BlockPos startPos, FlowingFluid fluid) {
        super(world, startPos, 2);  // TODO config
//        super(world, startPos, WPOConfig.COMMON.maxEqDist.get());
        this.fluid = fluid;
        FluidState fluidState = world.getFluidState(startPos);
        levelsAtStartPos = fluidState.getAmount();
    }

    @Override
    protected boolean skipProcessingStartPos() {
        // since current pos should eject levels, it is not valid initial pos to place levels into
        // therefore use all adjacent pos that are valid and can flow to
        return true;
    }

    @Override
    protected List<Direction> getNextDirections() {
        return FFluidStatic.getDirsRandomHorizontal(world.getRandom()); // ONLY horizontal
    }

    @Override
    boolean isValidPos(BlockPos pos) {
        FluidState fluidState = world.getFluidState(pos);
        BlockState blockState = world.getBlockState(pos);
        if (FFluidStatic.canHoldFluid(blockState)) {
            return fluid.isSame(fluidState.getType()) || fluidState.isEmpty();
        } else {
            return false;
        }
    }

    @Override
    void process(BlockPos pos) {
        FluidState state = world.getFluidState(pos);
        // push one level if higher than neighbor
        if (levelsAtStartPos > state.getAmount() + 1) { // at least two higher => equalize
            Tuple3<Boolean, Integer, FluidState> tuple3 = FFluidStatic.placeLevelsUpTo(state, fluid, 1);
            Boolean wasPlaced = tuple3.first;
            Integer placedLevels = tuple3.second;
            FluidState newFluidState = tuple3.third;
            if (wasPlaced) { // levels were actually placed
                levelsAtStartPos -= placedLevels;
                states.put(pos, newFluidState);
            } // else do nothing
        }
    }

    @Override
    Void finishComplete(int flags, int recursion) {
        return null; // never called: complete is always false
    }

    @Override
    Void finishNotComplete(int flags, int recursion) {
        if (!states.isEmpty()) { // fluid was moved from start pos => also update start pos state
            FluidState newStartPosFS = FFluidStatic.getSourceOrFlowingOrEmpty(fluid, levelsAtStartPos);
            states.put(startPos, newStartPosFS);
        }
        multiSetFluid(states, flags, recursion);
        return null;
    }
}
