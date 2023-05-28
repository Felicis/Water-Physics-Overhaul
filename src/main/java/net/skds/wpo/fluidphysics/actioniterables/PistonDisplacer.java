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
import net.minecraftforge.event.world.PistonEvent;
import net.minecraftforge.eventbus.api.Event;
import net.skds.wpo.fluidphysics.FFluidStatic;
import net.skds.wpo.util.Constants;

import java.util.HashSet;
import java.util.Set;

import static net.minecraft.state.properties.BlockStateProperties.WATERLOGGED;

public class PistonDisplacer implements IFluidActionIteratable {

    int maxFluidLevel = Constants.MAX_FLUID_LEVEL;
    // int bucketLevels = PhysEXConfig.MAX_FLUID_LEVEL;
    int remainingLvl;
    boolean complete = false;
    World world;
    Fluid fluid;
    // PistonBlockStructureHelper ps;
    Set<BlockPos> movepos = new HashSet<>();
    PistonEvent.Pre event;
    Long2ObjectLinkedOpenHashMap<BlockState> states = new Long2ObjectLinkedOpenHashMap<>();
    BlockState oldBS;

    public PistonDisplacer(World w, PistonEvent.Pre e, BlockState os, PistonBlockStructureHelper ps) {
        this.oldBS = os;
        FluidState oldFS = oldBS.getFluidState();
        // this.ps = ps;
        this.fluid = oldFS.getType();
        this.remainingLvl = oldFS.getAmount();
        this.world = w;
        this.event = e;
        movepos.addAll(ps.getToDestroy());
        movepos.addAll(ps.getToPush());
        for (BlockPos p : ps.getToPush()) {
            movepos.add(p.relative(event.getDirection())); // add block after the pushed one
            // System.out.println(p.relative(event.getDirection()));
        }
    }

    @Override
    public boolean isComplete() {
        return complete;
    }

    @Override
    public void addZero(Set<BlockPos> set, BlockPos p0) {
        for (Direction d : FFluidStatic.getRandomizedDirections(world.getRandom(), true)) {
            BlockPos pos2 = p0.relative(d);
            BlockState state2 = world.getBlockState(pos2);
            if (isValidState(state2) && FFluidStatic.canReach(p0, pos2, oldBS, state2, fluid, world)) {
                set.add(pos2);
            }
        }
    }

    @Override
    public void run(BlockPos pos, BlockState state) {
        // world.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5,
        // pos.getZ() + 0.5, 0, 0, 0);

        // if (fb) {
        // fb = false;
        // return;
        // }

        if (false && state.hasProperty(WATERLOGGED) && !state.getValue(WATERLOGGED)) {
            states.clear();
            states.put(pos.asLong(), FFluidStatic.forceApplyFluid(state, maxFluidLevel, (FlowingFluid) fluid));
            complete = true;
            return;
        }
        FluidState fs = state.getFluidState();
        int freeLvl = maxFluidLevel - fs.getAmount();
        remainingLvl -= freeLvl;
        int newLvl;
        if (remainingLvl <= 0) {
            newLvl = maxFluidLevel + remainingLvl;
            complete = true;
        } else {
            newLvl = maxFluidLevel;
        }
        if (freeLvl != 0) // something has been added
            states.put(pos.asLong(), FFluidStatic.forceApplyFluid(state, newLvl, (FlowingFluid) fluid));
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public boolean isValidState(BlockState state) {
        return fluid.isSame(state.getFluidState().getType()) || state.getFluidState().isEmpty();
    }

    @Override
    public boolean isValidPos(BlockPos pos) {
        return !movepos.contains(pos);
    }

    @Override
    public void finish() {
        ActionIterableUtils.fillStates(states, world);
        event.setResult(Event.Result.ALLOW);
        // System.out.println("u");
    }

    @Override
    public void fail() {
        event.setCanceled(true);
        // event.setResult(Result.DENY);
        // System.out.println("x");
    }
}
