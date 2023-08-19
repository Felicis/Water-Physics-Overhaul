package net.skds.wpo.fluidphysics.flowgraphiterators;

import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.skds.wpo.fluidphysics.FluidStatic;
import net.skds.wpo.util.tuples.Tuple2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FlowGraphBuilder {
    World world;
    BlockPos startPos;
    int maxRange;

    public FlowGraphBuilder(World world, BlockPos startPos, int maxRange) {
        this.world = world;
        this.startPos = startPos;
        this.maxRange = maxRange;
    }

    private boolean canHoldFluid(BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        return FluidStatic.canHoldFluid(blockState); // has to be able to flow through
    }

    private List<Direction> getNextDirections() {
        return FluidStatic.getDirsDownRandomHorizontal(world.getRandom()); // fluid does not flow UP
    }

    public Tuple2<Graph<BlockPos>, Set<BlockPos>> computeFlowGraphAndBorder() {
        World world = this.world;
        // create graph and add starting pos
        MutableGraph<BlockPos> flowGraph = GraphBuilder.directed().allowsSelfLoops(false).build();
        flowGraph.addNode(startPos);
        // all pos's which block the fluid flow in any way (opaque blocks or blocks constricting flow in some directions)
        Set<BlockPos> borderPoses = new HashSet<>();
        if (!canHoldFluid(startPos)) { // check for invalid start pos
            return new Tuple2<>(null, null); // ... maybe return empty graph and list?
        }
        // pos's for current and next range sweep
        List<BlockPos> currentList = new ArrayList<>();
        currentList.add(startPos); // TODO assert/check startPos isValid
        List<BlockPos> nextList = new ArrayList<>();
        // all visited pos's (visited means pos was reached & can hold fluid => was added to graph)
        List<BlockPos> allVisited = new ArrayList<>(currentList);

        // do range sweeps (breadth first)
        for (int range = 0; range <= maxRange; range++) { // start at range 0 (initial pos's)
            if (currentList.isEmpty()) { // if empty, we covered entire valid volume
                return new Tuple2<>(flowGraph, borderPoses);
            }
            for (BlockPos posCurrent : currentList) {
                if (range < maxRange) { // if not max range reached  => generate new pos's
                    // first down, then random sides, then up // TODO for taking maybe up, sides, down?
                    for (Direction nextDir : getNextDirections()) {
                        BlockPos posNew = posCurrent.relative(nextDir);
                        if (!allVisited.contains(posNew)) { // do not visit twice
                            allVisited.add(posNew);
                            if (FluidStatic.canFlowAndHold(world, posCurrent, nextDir)) {
                                // add node to graph
                                flowGraph.addNode(posNew);
                                flowGraph.putEdge(posCurrent, posNew); // no self loops because allVisited check
                                nextList.add(posNew); // set to visit next
                            } else { // border pos: blocks fluid flow
                                borderPoses.add(posNew);
                            }
                        }
                    }
                }
            }
            // continue with next set of pos's
            currentList.clear();
            currentList.addAll(nextList);
            nextList.clear();
        }
        // finished
        return new Tuple2<>(flowGraph, borderPoses); // immutable graph?
    }
}
