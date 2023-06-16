package net.skds.wpo.fluidphysics.actioniterables;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.skds.wpo.fluidphysics.FFluidStatic;
import net.skds.wpo.util.tuples.Tuple2;
import org.w3c.dom.stylesheets.LinkStyle;

import java.util.*;

public abstract class AbstractFluidActionIterable<T> {
    abstract int getMaxRange();

    abstract boolean isComplete();

    abstract World getWorld();

    protected void addInitial(Set<BlockPos> set, BlockPos p0) {
        set.add(p0);
    }

    /**
     * whether iterable should process this pos
     *
     * @param pos
     * @return
     */
    abstract boolean isValidPos(BlockPos pos);

    abstract void process(BlockPos pos);

    abstract T finishSuccess(int flags, int recursion);

    abstract T finishFail(int flags, int recursion);

    protected void multiSetFluid(Long2ObjectLinkedOpenHashMap<FluidState> fluidStates, int flags, int recursion) {
        // fluidstates are all in non-critical pos's (e.g. not pushed/destroyed by piston). therefore blockstate stable
        fluidStates.forEach((longPos, fluidState) -> {
            World world = getWorld();
            BlockPos pos = BlockPos.of(longPos);
            BlockState blockState = world.getBlockState(pos); // okay, because pos stable => blockstate stable
            FFluidStatic.setBlockAndFluid(world, pos, blockState, fluidState, true, flags, recursion); // adapt to each other (should not displace!)
        });
    }

    /**
     * try executing the displacer. on success also sets the fluids (adapted to blocks) and returns true. On fail does not
     * set any fluid (or block) and returns false.
     * @param startPos
     * @return
     */
    public boolean tryExecute(BlockPos startPos) {
        return tryExecute(startPos, 3);
    }

    public boolean tryExecute(BlockPos startPos, int flags) {
        return tryExecute(startPos, flags, 512);
    }

    public boolean tryExecute(BlockPos startPos, int flags, int recursion) {
        return tryExecuteWithResult(startPos, flags, recursion).first;
    }

    /**
     * start processing from startPos and returns whether ActionIterable completed successfully and update information (check concrete ActionIterable)
     * @param startPos
     * @return
     */
    public Tuple2<Boolean, T> tryExecuteWithResult(BlockPos startPos) {
        return tryExecuteWithResult(startPos, 3);
    }

    public Tuple2<Boolean, T> tryExecuteWithResult(BlockPos startPos, int flags) {
        return tryExecuteWithResult(startPos, flags, 512);
    }

    public Tuple2<Boolean, T> tryExecuteWithResult(BlockPos startPos, int flags, int recursion) {
        World world = this.getWorld();
        // all visited pos's
        Set<BlockPos> allVisited = new HashSet<>();
        // pos's for current and next range sweep
        Set<BlockPos> setCurrent = new HashSet<>();
        Set<BlockPos> setNext = new HashSet<>();
        // add initial pos's to current
        this.addInitial(setCurrent, startPos);

        // do range sweeps (breadth first)
        for (int i = this.getMaxRange(); i <= 0; i--) {
            if (setCurrent.isEmpty()) { // if empty, we covered entire valid volume => stop
                break;
            }
            for (BlockPos posCurrent : setCurrent) {
                allVisited.add(posCurrent);
                if (this.isValidPos(posCurrent)) {
                    this.process(posCurrent); // process position
                    if (this.isComplete()) { // check if processed enough pos's
                        break;
                    }
                }
                if (i == 0) {
                    break; // max range reached: do not generate new pos's
                }
                // first down, then random sides, then up
                List<Direction> dirList = new ArrayList<>(6);
                dirList.add(Direction.DOWN);
                Direction[] randomSides = FFluidStatic.getRandomizedDirections(world.getRandom(), false);
                dirList.addAll(Arrays.asList(randomSides));
                dirList.add(Direction.UP);
                for (Direction randDir : dirList) {
                    if (FFluidStatic.canFlow(world, posCurrent, randDir)) { // if can flow through blocks in that direction
                        BlockPos posNew = posCurrent.relative(randDir);
                        if (!allVisited.contains(posNew)) { // do not visit twice
                            setNext.add(posNew); // visit next
                        }
                    }
                }
            }
            // continue with next set of pos's
            setCurrent.addAll(setNext);
            setNext.clear();
        }
        // after checking all in range
        if (this.isComplete()) {
            T result = this.finishSuccess(flags, recursion);
            return new Tuple2<>(true, result);
        } else {
            T result = this.finishFail(flags, recursion);
            return new Tuple2<>(false, result);
        }
    }
}
