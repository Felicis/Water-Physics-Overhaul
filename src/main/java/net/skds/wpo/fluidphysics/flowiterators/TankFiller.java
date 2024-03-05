package net.skds.wpo.fluidphysics.flowiterators;

import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.skds.wpo.config.WPOConfig;
import net.skds.wpo.fluidphysics.FluidStatic;
import net.skds.wpo.fluidphysics.Constants;
import net.skds.wpo.util.tuples.Tuple2;

import java.util.HashMap;
import java.util.Map;

public class TankFiller extends AbstractFlowIterator<Integer> {

    int maxTankLevels;
    int levelsUntilFull;
    FlowingFluid fluid;
    IFluidHandlerItem fluidHandlerItem;
    Map<BlockPos, FluidState> states = new HashMap<>();
    Map<BlockPos, BlockPos> backwardsFlowMap = new HashMap<>();

    /**
     * fills task as much as possible from connected fluids starting at target pos<br/>
     * tryExecuteWithResult returns whether fully filling succeeded and the new tank level
     *
     * @param world
     * @param fluid
     * @param fluidHandlerItem
     */
    public TankFiller(World world, BlockPos startPos, FlowingFluid fluid, IFluidHandlerItem fluidHandlerItem) {
        super(world, startPos, WPOConfig.SERVER.maxBucketDist.get());
        this.fluid = fluid;
        this.fluidHandlerItem = fluidHandlerItem;
        maxTankLevels = this.fluidHandlerItem.getTankCapacity(0) / Constants.MILLIBUCKETS_PER_LEVEL;
        int currentLevel = this.fluidHandlerItem.getFluidInTank(0).getAmount() / Constants.MILLIBUCKETS_PER_LEVEL;
        // TODO config max levels until full?
        levelsUntilFull = maxTankLevels - currentLevel;
    }

    @Override
    protected void cacheFlow(BlockPos fromPos, BlockPos toPos) {
        // cache flow backwards
        backwardsFlowMap.put(toPos.immutable(), fromPos.immutable());
    }

    @Override
    protected boolean isValidPos(BlockPos pos) {
        FluidState fluidState = world.getFluidState(pos);
        boolean correctFluid = fluid.isSame(fluidState.getType()); // implies not empty and not zero levels
        if (correctFluid) {
            if (backwardsFlowMap.containsKey(pos)) { // if has previous pos => check if previous pos had fluid
                BlockPos previousPos = backwardsFlowMap.get(pos);
                boolean disconnectedFluid = world.getFluidState(previousPos).isEmpty(); // only slurp when fluids connected to start pos
                return !disconnectedFluid;
            } else { // first pos has no previous => ok
                return true;
            }
        } else { // wrong fluid
            return false;
        }
    }

    @Override
    protected void process(BlockPos pos) {
        Tuple2<Integer, FluidState> takenLvlsAndNewFS = FluidStatic.takeLevelsUpTo(world.getFluidState(pos), fluid, levelsUntilFull);
        Integer takenLevels = takenLvlsAndNewFS.first;
        FluidState newFluidState = takenLvlsAndNewFS.second;
        if (takenLevels > 0) { // levels were actually taken
            levelsUntilFull -= takenLevels;
            isComplete = (levelsUntilFull == 0);
            states.put(pos, newFluidState);
        } // else do nothing
    }

    @Override
    protected Integer finishComplete(int flags, int recursion) {
        return finish(flags, recursion); // same as fail
    }

    @Override
    protected Integer finishNotComplete(int flags, int recursion) {
        return finish(flags, recursion); // same as success
    }

    private Integer finish(int flags, int recursion) {
        multiSetFluid(states, flags, recursion);
        return maxTankLevels - levelsUntilFull; // return new tank level
    }
}
