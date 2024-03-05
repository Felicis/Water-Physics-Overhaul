package net.skds.wpo.fluidphysics.flowiterators;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.skds.wpo.config.WPOConfig;
import net.skds.wpo.fluidphysics.FluidStatic;
import net.skds.wpo.util.tuples.Tuple3;

import java.util.HashMap;
import java.util.Map;

/**
 * displaces fluid from a position. Run this before placing the block, which displaces the fluid (otherwise fluid can not escape the block and flow away).
 */
public class FluidDisplacer extends AbstractFlowIterator<Void> {

    int levelsToPlace;
    FlowingFluid fluid;
    Map<BlockPos, FluidState> states = new HashMap<>();

    public FluidDisplacer(World world, BlockPos startPos, FluidState oldFluidState) {
        super(world, startPos, WPOConfig.SERVER.maxDisplaceDist.get());
        if (oldFluidState.isEmpty()) {
            isComplete = true; // no fluid to displace
        } else {
            fluid = (FlowingFluid) oldFluidState.getType(); // cast safe because not empty
            levelsToPlace = oldFluidState.getAmount();
        }
    }


    @Override
    protected boolean isValidPos(BlockPos pos) {
        FluidState fluidState = world.getFluidState(pos);
        BlockState blockState = world.getBlockState(pos);
        if (FluidStatic.canHoldFluid(blockState)) {
            return fluid.isSame(fluidState.getType()) || fluidState.isEmpty();
        } else {
            return false;
        }
    }

    @Override
    protected boolean skipProcessingStartPos() {
        // since current pos should displace, it is not valid initial pos,
        // therefore use all adjacent pos that are valid and can flow to
        return true;
    }

    @Override
    protected void process(BlockPos pos) {
        FluidState state = world.getFluidState(pos);
        Tuple3<Boolean, Integer, FluidState> tuple3 = FluidStatic.placeLevelsUpTo(state, fluid, levelsToPlace);
        Boolean wasPlaced = tuple3.first;
        Integer placedLevels = tuple3.second;
        FluidState newFluidState = tuple3.third;
        if (wasPlaced) { // levels were actually placed
            levelsToPlace -= placedLevels;
            isComplete = (levelsToPlace == 0);
            states.put(pos, newFluidState);
        } // else do nothing
    }

    @Override
    protected Void finishComplete(int flags, int recursion) {
        if (!states.isEmpty()) { // fluid was moved from start pos => also update start pos state
            states.put(startPos, Fluids.EMPTY.defaultFluidState());
        }
        multiSetFluid(states, flags, recursion);
        return null;
    }

    @Override
    protected Void finishNotComplete(int flags, int recursion) {
        return null;
    }
}
