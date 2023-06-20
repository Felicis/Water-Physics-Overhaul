package net.skds.wpo.fluidphysics.actioniterables;

import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.skds.wpo.WPOConfig;
import net.skds.wpo.fluidphysics.FFluidStatic;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LedgeFinder extends AbstractFluidActionIterable<Direction> {
    boolean complete = false;
    World world;
    BlockPos startPos;
    BlockPos afterLedgeDropPos;

    Map<BlockPos, BlockPos> backwardsFlowMap = new HashMap<>();

    /**
     * finds closest ledge, where fluid can flow one level down (could also be starting pos => direct down flow)<br/>
     * no fluid is moved, no checks whether flow path is blocked by other fluid, only canFlow and canHold<br/>
     * tryExecuteWithResult returns whether ledge found and the direction from starting pos to reach the ledge
     * @param world
     */
    public LedgeFinder(World world) {
        this.world = world;
    }

    @Override
    int getMaxRange() {
        return WPOConfig.COMMON.maxSlideDist.get() + 1; // because iterable has to go down off ledge
    }

    @Override
    boolean isComplete() {
        return complete;
    }

    @Override
    World getWorld() {
        return world;
    }

    @Override
    protected void cacheFlow(BlockPos fromPos, BlockPos toPos) {
        // cache flow backwards
        backwardsFlowMap.put(toPos, fromPos);
    }

    @Override
    boolean isValidPos(BlockPos pos) {
        // INFO pos can be higher or lower than startPos, but canFlow was passed
        if (notHigherThanStartPos(pos)) { // has to be same height or lower
            BlockState blockState = world.getBlockState(pos);
            return FFluidStatic.canHoldFluid(blockState); // has to be able to flow through
            // different fluids could block each other, but they all want to flow to ledge, so ok
        } else {
            return false;
        }
    }

    private boolean notHigherThanStartPos(BlockPos pos) {
        return pos.getY() <= startPos.getY();
    }

    @Override
    protected void addInitial(Set<BlockPos> set, BlockPos pos0) {
        // store inital pos
        startPos = pos0;
        set.add(pos0);
    }

    @Override
    void process(BlockPos pos) {
        // INFO pos can be higher or lower than startPos, but canFlow and isValidPos were passed
        if (isLowerThanStartPos(pos)) {
            // found ledge!!
            afterLedgeDropPos = pos;
            complete = true;
        }
    }

    private boolean isLowerThanStartPos(BlockPos pos) {
        return pos.getY() < startPos.getY();
    }

    @Override
    Direction finishSuccess(int flags, int recursion) {
        // follow backwards flow until start position and return starting direction (for flow to reach ledge)
        BlockPos toPos = afterLedgeDropPos;
        BlockPos fromPos = backwardsFlowMap.get(toPos); // flow backwards (afterLedgeDropPos can not be starting)
        while (!fromPos.equals(startPos)) {
            toPos = fromPos;
            fromPos = backwardsFlowMap.get(toPos); // flow backwards
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
    Direction finishFail(int flags, int recursion) {
        return null; // no ledge found
    }
}
