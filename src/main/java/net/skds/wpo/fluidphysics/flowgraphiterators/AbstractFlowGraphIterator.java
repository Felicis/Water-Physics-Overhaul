package net.skds.wpo.fluidphysics.flowgraphiterators;

import com.google.common.graph.Graph;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.skds.wpo.config.WPOConfig;
import net.skds.wpo.fluidphysics.FluidStatic;
import net.skds.wpo.fluidphysics.FluidFlowCache;
import net.skds.wpo.util.tuples.Tuple2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractFlowGraphIterator<T> {
    World world;
    Graph<BlockPos> flowGraph;
    BlockPos startPos;
    int maxRange;
    boolean isComplete = false; // set in subclass when execution should stop and which finish method to call after execution

    /**
     * @param startPos
     * @param maxRange - max range from startPos for execution (usually config)
     */
    protected AbstractFlowGraphIterator(World world, BlockPos startPos, int maxRange) {
        this.world = world;
        this.startPos = startPos;
        this.maxRange = maxRange;
        // check cache or recompute
        FluidFlowCache flowCache = FluidStatic.getFlowCache(world);
        Tuple2<Boolean, Tuple2<Graph<BlockPos>, Set<BlockPos>>> cacheHitAndData = flowCache.getFlowGraphAndBorder(startPos);
        if (cacheHitAndData.first) { // cache hit => use cached flow
            this.flowGraph = cacheHitAndData.second.first;
        } else { // cache miss => recompute flow
            FlowGraphBuilder flowGraphBuilder = new FlowGraphBuilder(world, startPos, WPOConfig.SERVER.getMaxDist());
            Tuple2<Graph<BlockPos>, Set<BlockPos>> flowGraphAndBorder = flowGraphBuilder.computeFlowGraphAndBorder();
            this.flowGraph = flowGraphAndBorder.first;
            // update cache
            flowCache.setFlowGraphAndBorder(startPos, flowGraphAndBorder);
        }
    }

    /**
     * override to skip processing (e.g. fluid displacer skips start position)
     *
     * @return
     */
    protected boolean skipProcessingStartPos() {
        return false;
    }

    /**
     * whether iterable should process this pos
     *
     * @param pos
     * @return
     */
    abstract boolean shouldContinueFlow(BlockPos pos);

    abstract void process(BlockPos pos);

    /**
     * called after execution if getComplete() returned true (execution stops if getComplete changes to true)
     *
     * @param flags
     * @param recursion
     * @return
     */
    abstract T finishComplete(int flags, int recursion);

    /**
     * called after execution if getComplete() returned false
     *
     * @param flags
     * @param recursion
     * @return
     */
    abstract T finishNotComplete(int flags, int recursion);

    protected void multiSetFluid(Map<BlockPos, FluidState> fluidStates, int flags, int recursion) {
        // fluidstates are all in non-critical pos's (e.g. not pushed/destroyed by piston). therefore blockstate stable
        fluidStates.forEach((pos, fluidState) -> {
            FluidStatic.setFluidAlsoBlock(world, pos, fluidState, flags, recursion);
        });
    }

    /**
     * try executing. on success also sets the fluids (adapted to blocks) and returns true. On fail does not
     * set any fluid (or block) and returns false.
     *
     * @return
     */
    public boolean tryExecute() {
        return tryExecute(3);
    }

    public boolean tryExecute(int flags) {
        return tryExecute(flags, 512);
    }

    public boolean tryExecute(int flags, int recursion) {
        return tryExecuteWithResult(flags, recursion).first;
    }

    /**
     * start executing and return whether ActionIterable completed successfully and update information (check concrete ActionIterable)
     *
     * @return
     */
    public Tuple2<Boolean, T> tryExecuteWithResult() {
        return tryExecuteWithResult(3);
    }

    public Tuple2<Boolean, T> tryExecuteWithResult(int flags) {
        return tryExecuteWithResult(flags, 512);
    }

    public Tuple2<Boolean, T> tryExecuteWithResult(int flags, int recursion) {
        if (tryExecuteWithResultImpl()) {
            T result = this.finishComplete(flags, recursion);
            return new Tuple2<>(true, result);
        } else {
            T result = this.finishNotComplete(flags, recursion);
            return new Tuple2<>(false, result);
        }
    }

    private boolean tryExecuteWithResultImpl() {
        // check if execute not needed (subclass constructor can set this)
        if (isComplete) {
            return true;
        }
        // pos's for current and next range sweep
        List<BlockPos> currentList = new ArrayList<>();
        currentList.add(startPos); // TODO assert/check startPos isValid
        List<BlockPos> nextList = new ArrayList<>();

        // do range sweeps (breadth first)
        for (int range = 0; range <= maxRange; range++) { // start at range 0 (initial pos's)
            if (currentList.isEmpty()) { // if empty, we covered entire valid volume & not found
                return false;
            }
            for (BlockPos posCurrent : currentList) {
                if (!(range == 0 && skipProcessingStartPos())) { // if NOT (!) range == 0 and skip start
                    this.process(posCurrent); // process position
                    if (isComplete) { // check if processed enough pos's
                        return true;
                    }
                }
                if (range < maxRange) { // if not max range reached  => generate new pos's
                    // directions depend on graph (currently only sides or down)
                    if (flowGraph.nodes().contains(posCurrent)) { // check node exists, else successors() exception
                        for (BlockPos posNew : flowGraph.successors(posCurrent)) {
                            if (this.shouldContinueFlow(posNew)) {
                                nextList.add(posNew); // set to visit next
                            }
                        }
                    } else {
                        int asdf = 3;
                    }
                }
            }
            // continue with next set of pos's
            currentList.clear();
            currentList.addAll(nextList);
            nextList.clear();
        }
        // finished before completion
        return false;
    }
}
