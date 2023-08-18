package net.skds.wpo.fluidphysics.actioniterables.graphs;

import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.skds.wpo.WPOConfig;

import java.util.HashMap;
import java.util.Map;

public class LedgeFinder2 extends AbstractFluidActionIterable2<Direction> {
    BlockPos afterLedgeDropPos;

    /**
     * finds closest ledge, where fluid can flow one level down (could also be starting pos => direct down flow)<br/>
     * no fluid is moved, no checks whether flow path is blocked by other fluid, only canFlow and canHold<br/>
     * tryExecuteWithResult returns whether ledge found and the direction from starting pos to reach the ledge
     *
     * @param world
     */
    public LedgeFinder2(World world, BlockPos startPos) {
        super(world, startPos, WPOConfig.COMMON.maxSlideDist.get() + 1); // +1 because iterable has to go down after ledge
    }

    @Override
    boolean shouldContinueFlow(BlockPos pos) {
        // do not equalize up
        return pos.getY() <= startPos.getY();
        // different fluids could block each other, but they all want to flow to ledge, so ok
    }

    @Override
    void process(BlockPos pos) {
        // if lower than start pos
        if (pos.getY() < startPos.getY()) {
            // found ledge!!
            afterLedgeDropPos = pos;
            isComplete = true;
        }
    }

    @Override
    Direction finishComplete(int flags, int recursion) {
        // follow backwards flow until start position and return starting direction (for flow to reach ledge)
        BlockPos toPos = afterLedgeDropPos;
        // step at least once: afterLedgeDropPos can not be starting
        BlockPos fromPos = flowGraph.predecessors(toPos).iterator().next(); // no cycles => only 1 predecessor
        while (!fromPos.equals(startPos)) {
            toPos = fromPos;
            fromPos = flowGraph.predecessors(toPos).iterator().next(); // flow backwards
        }
        return getDirection(fromPos, toPos);
    }

    private Direction getDirection(BlockPos fromPos, BlockPos toPos) {
        // signum to get direction without having to normalize (fromNormal only takes normalized coords)
        int dx = Integer.signum(toPos.getX() - fromPos.getX());
        int dy = Integer.signum(toPos.getY() - fromPos.getY());
        int dz = Integer.signum(toPos.getZ() - fromPos.getZ());
        return Direction.fromNormal(dx, dy, dz);
    }

    @Override
    Direction finishNotComplete(int flags, int recursion) {
        return null; // no ledge found
    }
}
