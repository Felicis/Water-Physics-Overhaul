package net.skds.wpo.fluidphysics.flowiterators;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.skds.wpo.config.WPOConfig;
import net.skds.wpo.fluidphysics.FluidStatic;
import net.skds.wpo.util.tuples.Tuple3;

import java.util.HashMap;
import java.util.Map;

public class TankFlusher extends AbstractFlowIterator<Integer> {

    int initalTankLevel;
    int levelsToPlace;
    FlowingFluid tankFluid;
    Map<BlockPos, FluidState> states = new HashMap<>();

    /**
     * empties tank as much as possible starting at target pos<br/>
     * tryExecuteWithResult returns whether completely emptying succeeded and the new tank level
     *
     * @param world
     * @param startPos
     * @param tankFluid
     * @param levelsToPlace
     */
    public TankFlusher(World world, BlockPos startPos, FlowingFluid tankFluid, int levelsToPlace) {
        super(world, startPos, WPOConfig.SERVER.maxBucketDist.get());
        this.tankFluid = tankFluid;
        this.levelsToPlace = levelsToPlace;
    }

    @Override
    protected boolean isValidPos(BlockPos pos) {
        FluidState fluidState = world.getFluidState(pos);
        BlockState blockState = world.getBlockState(pos);
        if (FluidStatic.canHoldFluid(blockState)) {
            return tankFluid.isSame(fluidState.getType()) || fluidState.isEmpty();
        } else {
            return false;
        }
    }

    @Override
    protected void process(BlockPos pos) {
        FluidState state = world.getFluidState(pos);
        Tuple3<Boolean, Integer, FluidState> tuple3 = FluidStatic.placeLevelsUpTo(state, tankFluid, levelsToPlace);
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
    protected Integer finishComplete(int flags, int recursion) {
        return finish(flags, recursion); // same as success
    }

    @Override
    protected Integer finishNotComplete(int flags, int recursion) {
        return finish(flags, recursion); // same as success
    }

    private Integer finish(int flags, int recursion) {
        multiSetFluid(states, flags, recursion);
        return initalTankLevel - levelsToPlace; // return new tank level
    }
}
