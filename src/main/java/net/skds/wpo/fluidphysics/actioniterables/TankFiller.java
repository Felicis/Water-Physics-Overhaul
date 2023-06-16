package net.skds.wpo.fluidphysics.actioniterables;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.skds.wpo.WPOConfig;
import net.skds.wpo.fluidphysics.FFluidStatic;
import net.skds.wpo.util.tuples.Tuple2;

public class TankFiller extends AbstractFluidActionIterable<Integer> {

    int maxTankLevels;
    int levelsUntilFull;
    boolean complete = false;
    World world;
    FlowingFluid fluid;
    IFluidHandlerItem fluidHandlerItem;
    Long2ObjectLinkedOpenHashMap<FluidState> states = new Long2ObjectLinkedOpenHashMap<>();

    /**
     * fills task as much as possible from connected fluids starting at target pos<br/>
     * tryExecuteWithResult returns whether fully filling succeeded and the new tank level
     * @param world
     * @param fluid
     * @param fluidHandlerItem
     */
    public TankFiller(World world, FlowingFluid fluid, IFluidHandlerItem fluidHandlerItem) {
        this.world = world;
        this.fluid = fluid;
        this.fluidHandlerItem = fluidHandlerItem;
        maxTankLevels = this.fluidHandlerItem.getTankCapacity(0) / FFluidStatic.MILLIBUCKETS_PER_LEVEL;
        int currentLevel = this.fluidHandlerItem.getFluidInTank(0).getAmount() / FFluidStatic.MILLIBUCKETS_PER_LEVEL;
        // TODO config max levels until full?
        levelsUntilFull = maxTankLevels - currentLevel;
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
        Tuple2<Integer, FluidState> takenLvlsAndNewFS = FFluidStatic.takeLevelsUpTo(world.getFluidState(pos), levelsUntilFull);
        Integer takenLevels = takenLvlsAndNewFS.first;
        FluidState newFluidState = takenLvlsAndNewFS.second;
        if (takenLevels > 0) { // levels were actually taken
            levelsUntilFull -= takenLevels;
            complete = (levelsUntilFull == 0);
            states.put(pos.asLong(), newFluidState);
        } // else do nothing
    }

    @Override
    protected Integer finishSuccess(int flags, int recursion) {
        return finish(flags, recursion); // same as fail
    }

    @Override
    protected Integer finishFail(int flags, int recursion) {
        return finish(flags, recursion); // same as success
    }

    private Integer finish(int flags, int recursion) {
        multiSetFluid(states, flags, recursion);
        return maxTankLevels - levelsUntilFull; // return new tank level
    }
}
