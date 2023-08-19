package net.skds.wpo.fluidphysics;

import com.google.common.graph.Graph;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.skds.wpo.util.tuples.Tuple2;

import javax.annotation.Nonnull;
import java.util.*;

public class FluidFlowCache {
    // store flow graph and backwards maps (also border around flow)
    private final Map<BlockPos, Tuple2<Graph<BlockPos>, Set<BlockPos>>> startPos2FlowAndBorder = new HashMap<>();
    private final Map<BlockPos, Set<BlockPos>> border2startPos = new HashMap<>();
    private final Map<BlockPos, Set<BlockPos>> flow2startPoses = new HashMap<>();
    private final Map<BlockPos, Direction> downhillFlowDirection = new HashMap<>();

    public FluidFlowCache() {
    }

    /**
     * returns if was cache hit, and the cached poses that this pos can flow to/reach
     * @param pos
     * @return
     */
    public Tuple2<Boolean, Tuple2<Graph<BlockPos>, Set<BlockPos>>> getFlowGraphAndBorder(@Nonnull BlockPos pos) {
        return new Tuple2<>(startPos2FlowAndBorder.containsKey(pos), startPos2FlowAndBorder.getOrDefault(pos, null));
    }

    public void setFlowGraphAndBorder(BlockPos pos, Tuple2<Graph<BlockPos>, Set<BlockPos>> graphAndBorder) {
        invalidateFlowFrom(pos);
        // store flow and border
        startPos2FlowAndBorder.put(pos.immutable(), graphAndBorder);
        // update backwards maps
        for (BlockPos flowPos: graphAndBorder.first.nodes()) { // add pos to new flow targets
            if (!flow2startPoses.containsKey(flowPos)) { // create list if needed
                flow2startPoses.put(flowPos.immutable(), new HashSet<>());
            }
            flow2startPoses.get(flowPos).add(pos); // add to list in map
        }
        for (BlockPos borderPos: graphAndBorder.second) { // add pos to new borders
            if (!border2startPos.containsKey(borderPos)) { // create list if needed
                border2startPos.put(borderPos.immutable(), new HashSet<>());
            }
            border2startPos.get(borderPos).add(pos); // add to list in map
        }
    }

    /**
     * returns list of all starting poses of flows which can flow to given pos.
     * @param pos
     * @return
     */
    public Set<BlockPos> getFluidsInRangeOf(BlockPos pos) {
        return flow2startPoses.getOrDefault(pos, new HashSet<>());
    }

    private void invalidateFlowFrom(BlockPos pos) {
        if (startPos2FlowAndBorder.containsKey(pos)) { // if flow was already cached
            Tuple2<Graph<BlockPos>, Set<BlockPos>> flowAndBorder = startPos2FlowAndBorder.get(pos);
            // clear backwards maps (not only map from which the invalidation started)
            for (BlockPos flowPos : flowAndBorder.first.nodes()) { // remove all flow targets from backwards map
                if (flow2startPoses.containsKey(flowPos)) {
                    flow2startPoses.get(flowPos).remove(pos);
                }
            }
            for (BlockPos borderPos : flowAndBorder.second) { // remove all borders from backwards map
                if (border2startPos.containsKey(borderPos)) {
                    border2startPos.get(borderPos).remove(pos);
                }
            }
            // clear graph
            startPos2FlowAndBorder.remove(pos);
        }
        // also clear cached downhill direction
        invalidateDownhillDirection(pos);
    }

    /**
     * invalidates all flows which reach or touch (border) toPos
     * @param toPos
     * @return the list of starting poses of all the flows which were invalidated
     */
    public List<BlockPos> invalidateFlowTo(BlockPos toPos) {
        List<BlockPos> invalidatedStartPoses = new LinkedList<>();
        // invalidate start poses which have toPos as flow or border
        if (flow2startPoses.containsKey(toPos)) {
            ArrayList<BlockPos> flowStartPoses = new ArrayList<>(flow2startPoses.get(toPos));
            for (BlockPos flowStartPos: flowStartPoses) { // iterate over new list, otherwise ConcurrentModificationException
                invalidateFlowFrom(flowStartPos);
            }
            invalidatedStartPoses.addAll(flowStartPoses);
        }
        if (border2startPos.containsKey(toPos)) {
            ArrayList<BlockPos> borderStartPoses = new ArrayList<>(border2startPos.get(toPos));
            for (BlockPos borderStartPos: borderStartPoses) { // iterate over new list, otherwise ConcurrentModificationException
                invalidateFlowFrom(borderStartPos);
            }
            invalidatedStartPoses.addAll(borderStartPoses);
        }
        return invalidatedStartPoses;
    }

    /**
     * returns if was cache hit, and which downhill direction was cached (null direction cache hit means no ledge found)
     * @param pos
     * @return
     */
    public Tuple2<Boolean, Direction> getDownhillDirection(@Nonnull BlockPos pos) {
        return new Tuple2<>(downhillFlowDirection.containsKey(pos), downhillFlowDirection.getOrDefault(pos, null));
    }

    public void setDownhillDirection(@Nonnull BlockPos pos, Direction dir) {
        downhillFlowDirection.put(pos.immutable(), dir); // overwrites old value
    }

    public void invalidateDownhillDirection(@Nonnull BlockPos pos) {
        downhillFlowDirection.remove(pos);
    }
}
