package net.skds.wpo.fluidphysics.actioniterables;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.LavaFluid;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.skds.wpo.fluidphysics.FFluidStatic;
import net.skds.wpo.util.tuples.Tuple3;

import java.util.Set;

public class FluidDisplacer extends AbstractFluidActionIterable<Void> {

    int levelsToPlace;
    boolean complete = false;
    World world;
    FlowingFluid fluid;
    Long2ObjectLinkedOpenHashMap<FluidState> states = new Long2ObjectLinkedOpenHashMap<>();

    public FluidDisplacer(World world, FluidState oldFluidState) {
        if (oldFluidState.isEmpty()) {
            complete = true; // no fluid to displace
        } else {
            fluid = (FlowingFluid) oldFluidState.getType(); // cast safe because not empty
            levelsToPlace = oldFluidState.getAmount();
            this.world = world;
        }
    }

    @Override
    protected boolean isComplete() {
        return complete;
    }

    @Override
    protected int getMaxRange() {
        return 10; // TODO make config: max fluid displace distance?
    }

    @Override
    protected World getWorld() {
        return world;
    }

    @Override
    protected boolean isValidPos(BlockPos pos) {
        FluidState fluidState = world.getFluidState(pos);
        BlockState blockState = world.getBlockState(pos);
        if (FFluidStatic.canHoldFluid(blockState)) {
            return fluid.isSame(fluidState.getType()) || fluidState.isEmpty();
        } else {
            return false;
        }
    }

    @Override
    protected void addInitial(Set<BlockPos> set, BlockPos pos0) {
        // since current pos should displace, it is not valid initial pos,
        // therefore use all adjacent pos that are valid and can flow to
        for (Direction randDir : FFluidStatic.getDirsDownRandomHorizontalUp(world.getRandom())) {
            BlockPos adjacentPos = pos0.relative(randDir);
            if (isValidPos(adjacentPos) && FFluidStatic.canFlow(world, pos0, randDir)) {
                set.add(adjacentPos);
            }
        }
    }

    @Override
    protected void process(BlockPos pos) {
        FluidState state = world.getFluidState(pos);
        Tuple3<Boolean, Integer, FluidState> tuple3 = FFluidStatic.placeLevelsUpTo(state, fluid, levelsToPlace);
        Boolean wasPlaced = tuple3.first;
        Integer placedLevels = tuple3.second;
        FluidState newFluidState = tuple3.third;
        if (wasPlaced) { // levels were actually placed
            levelsToPlace -= placedLevels;
            complete = (levelsToPlace == 0);
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
