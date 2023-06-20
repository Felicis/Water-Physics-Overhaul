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

    protected void cacheFlow(BlockPos fromPos, BlockPos toPos) {
        // nop
    }

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
        if (tryExecuteWithResultImpl(startPos, flags, recursion)) {
            T result = this.finishSuccess(flags, recursion);
            return new Tuple2<>(true, result);
        } else {
            T result = this.finishFail(flags, recursion);
            return new Tuple2<>(false, result);
        }
    }
    public boolean tryExecuteWithResultImpl(BlockPos startPos, int flags, int recursion) {
        // check if execute not needed
        if (this.isComplete()) {
            return true;
        }

        World world = this.getWorld();
        // pos's for current and next range sweep
        Set<BlockPos> setCurrent = new HashSet<>();
        Set<BlockPos> setNext = new HashSet<>();
        // add initial pos's to current (if valid)
        this.addInitial(setCurrent, startPos);
        setCurrent.removeIf(pos -> !this.isValidPos(pos)); // remove invalid pos
        // all visited pos's (visited means pos was reached and checked for validity)
        Set<BlockPos> allVisited = new HashSet<>(setCurrent);

        // do range sweeps (breadth first)
        for (int range = 0; range <= this.getMaxRange(); range++) { // start at range 0 (initial pos's)
            if (setCurrent.isEmpty()) { // if empty, we covered entire valid volume & not found
                return false;
            }
            for (BlockPos posCurrent : setCurrent) {
                this.process(posCurrent); // process position
                if (this.isComplete()) { // check if processed enough pos's
                    return true;
                }
                if (range < this.getMaxRange()) { // if not max range reached  => generate new pos's
                    // first down, then random sides, then up // TODO for taking maybe up, sides, down?
                    for (Direction randDir : FFluidStatic.getDirsDownRandomHorizontalUp(world.getRandom())) {
                        if (FFluidStatic.canFlow(world, posCurrent, randDir)) { // if can flow through blocks in that direction
                            BlockPos posNew = posCurrent.relative(randDir);
                            if (!allVisited.contains(posNew)) { // do not visit twice
                                allVisited.add(posCurrent);
                                cacheFlow(posCurrent, posNew); // needs to be before isValidPos
                                if (this.isValidPos(posCurrent)) { // only consider valid
                                    setNext.add(posNew); // set to visit next
                                }
                            }
                        }
                    }
                }
            }
            // continue with next set of pos's
            setCurrent.clear();
            setCurrent.addAll(setNext);
            setNext.clear();
        }
        // not found
        return false;
    }
}
