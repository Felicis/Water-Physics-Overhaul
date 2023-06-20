package net.skds.wpo.fluidphysics.actioniterables;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.skds.wpo.WPOConfig;
import net.skds.wpo.fluidphysics.FFluidStatic;
import net.skds.wpo.util.Constants;
import net.skds.wpo.util.tuples.Tuple3;

public class TankFlusher extends AbstractFluidActionIterable<Integer> {

    int initalTankLevel;
    int levelsToPlace;
    boolean complete = false;
    World world;
    FlowingFluid tankFluid;
    IFluidHandlerItem fluidHandler;
    Long2ObjectLinkedOpenHashMap<FluidState> states = new Long2ObjectLinkedOpenHashMap<>();

    /**
     * empties task as much as possible starting at target pos<br/>
     * tryExecuteWithResult returns whether completely emptying succeeded and the new tank level
     * @param world
     * @param fluidHandler
     */
    public TankFlusher(World world, IFluidHandlerItem fluidHandler) {
        this.fluidHandler = fluidHandler;
        if (this.fluidHandler.getFluidInTank(0).isEmpty()) {
            complete = true; // no fluid in tank
        } else {
            tankFluid = (FlowingFluid) this.fluidHandler.getFluidInTank(0).getFluid(); // safe cast because not empty
            this.world = world;
            // TODO config max levels to place?
            initalTankLevel = this.fluidHandler.getFluidInTank(0).getAmount() / Constants.MILLIBUCKETS_PER_LEVEL;
            levelsToPlace = initalTankLevel;
        }
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
        BlockState blockState = world.getBlockState(pos);
        if (FFluidStatic.canHoldFluid(blockState)) {
            return tankFluid.isSame(fluidState.getType()) || fluidState.isEmpty();
        } else {
            return false;
        }
    }

    @Override
    protected void process(BlockPos pos) {
        FluidState state = world.getFluidState(pos);
        Tuple3<Boolean, Integer, FluidState> tuple3 = FFluidStatic.placeLevelsUpTo(state, tankFluid, levelsToPlace);
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
    protected Integer finishSuccess(int flags, int recursion) {
        return finish(flags, recursion); // same as success
    }

    @Override
    protected Integer finishFail(int flags, int recursion) {
        return finish(flags, recursion); // same as success
    }

    private Integer finish(int flags, int recursion) {
        multiSetFluid(states, flags, recursion);
        return initalTankLevel - levelsToPlace; // return new tank level
    }
}
