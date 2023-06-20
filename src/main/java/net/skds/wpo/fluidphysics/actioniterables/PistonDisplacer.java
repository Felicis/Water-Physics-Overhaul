package net.skds.wpo.fluidphysics.actioniterables;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.PistonBlockStructureHelper;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.skds.wpo.fluidphysics.FFluidStatic;
import net.skds.wpo.util.tuples.Tuple3;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class PistonDisplacer extends AbstractFluidActionIterable<Void> {

    int levelsToPlace;
    boolean complete = false;
    World world;
    FlowingFluid fluid;
    Set<BlockPos> afterPush = new HashSet<>(); // contains pos left empty after push/pull (e.g.  of wooden planks, sticky, ...) => creates air
    Set<BlockPos> toDestroy = new HashSet<>(); // pos that are destroyed by piston (e.g. flower) -- air is not

    Set<BlockPos> piston = new HashSet<>(); // piston base and head
    Long2ObjectLinkedOpenHashMap<FluidState> states = new Long2ObjectLinkedOpenHashMap<>();

    public PistonDisplacer(World world, BlockPos pistonBasePos, Direction facing, boolean isExtend, FluidState fluidState, PistonBlockStructureHelper ps) {
        if (fluidState.isEmpty()) {
            complete = true; // no fluid to displace
        } else {
            this.fluid = (FlowingFluid) fluidState.getType(); // safe cast because not empty
            levelsToPlace = fluidState.getAmount();
            this.world = world;

            Direction moveDirection = isExtend ? facing : facing.getOpposite();
            // contains all blocks (ALSO fluidlogged) which are pushed (e.g. anvil)
            afterPush.addAll(ps.getToPush().stream().map(p -> p.relative(moveDirection)).collect(Collectors.toSet())); // toPush[].relative(moveDir)
            toDestroy.addAll(ps.getToDestroy()); // contains all blocks (ALSO fluidlogged) which are destroyed (e.g. Ladder, Fluid)
            // add piston base and piston head pos (not in afterPush and toDestroy)
            piston.add(pistonBasePos); // piston base pos (never allowed)
            if (isExtend) { // when extending: piston head not in afterPush or toDestroy, but not valid
                piston.add(pistonBasePos.relative(facing)); // piston head pos (destination)
            } // when contracting: piston head in afterPush (sticky/slime = blocked) or becomes empty (=valid) => no handling needed
            // TODO IDEA: fluidlog piston head? => double pump: need to process extending and retracting separately & set head pos as valid
        }
    }

    @Override
    protected boolean isComplete() {
        return complete;
    }

    @Override
    protected int getMaxRange() {
        return 12; // TODO add to config
    }

    @Override
    protected World getWorld() {
        return world;
    }

    @Override
    protected boolean isValidPos(BlockPos pos) {
        FluidState fluidState = world.getFluidState(pos);
        BlockState blockState = world.getBlockState(pos);
        // implicit filter: vacated pos's, i.e. spaces left empty (!afterPush) which were filled with block before push (toPush), which are not piston => allowed
        if (!afterPush.contains(pos) && !toDestroy.contains(pos) && !piston.contains(pos) && FFluidStatic.canHoldFluid(blockState)) {
            return fluid.isSame(fluidState.getType()) || fluidState.isEmpty();
        } else {
            return false;
        }
    }

    @Override
    protected void addInitial(Set<BlockPos> set, BlockPos pos0) {
        // since current pos will be destroyed by piston, it is not valid initial pos,
        // therefore use all adjacent pos that are valid and can flow to
        for (Direction randDir : FFluidStatic.getDirsDownRandomHorizontalUp(world.getRandom())) {
            BlockPos adjacentPos = pos0.relative(randDir);
            if (isValidPos(adjacentPos) && FFluidStatic.canFlow(world, pos0, randDir)) {
                set.add(adjacentPos);
            }
        }
    }

    @Override
    protected void process(BlockPos pos) {
        FluidState state = world.getFluidState(pos);
        Tuple3<Boolean, Integer, FluidState> tuple3 = FFluidStatic.placeLevelsUpTo(state, fluid, levelsToPlace);
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
    protected Void finishSuccess(int flags, int recursion) {
        multiSetFluid(states, flags, recursion);
        return null;
    }

    @Override
    protected Void finishFail(int flags, int recursion) {
        return null;
    }
}
