package net.skds.wpo.fluidphysics.actioniterables;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.skds.wpo.fluidphysics.FFluidStatic;
import net.skds.wpo.util.Constants;

import java.util.Set;

import static net.minecraft.state.properties.BlockStateProperties.WATERLOGGED;

public class FluidDisplacer implements IFluidActionIteratable {

    int mfl = Constants.MAX_FLUID_LEVEL;
    // int bucketLevels = PhysEXConfig.MAX_FLUID_LEVEL;
    int sl;
    boolean complete = false;
    World world;
    Fluid fluid;
    Long2ObjectLinkedOpenHashMap<BlockState> states = new Long2ObjectLinkedOpenHashMap<>();
    BlockState obs;

     public FluidDisplacer(World w, BlockState obs) {
        FluidState ofs = obs.getFluidState();
        obs = obs;
        fluid = ofs.getType();
        sl = ofs.getAmount();
        world = w;
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
            if (isValidState(state2) && FFluidStatic.canReach(p0, pos2, obs, state2, fluid, world)) {
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
            states.put(pos.asLong(), FFluidStatic.forceApplyFluid(state, mfl, (FlowingFluid) fluid));
            complete = true;
            return;
        }
        FluidState fs = state.getFluidState();
        int el = mfl - fs.getAmount();
        int osl = sl;
        sl -= el;
        int nl = mfl;
        if (sl <= 0) {
            nl = mfl + sl;
            complete = true;
        }
        if (osl != sl)
            states.put(pos.asLong(), FFluidStatic.forceApplyFluid(state, nl, (FlowingFluid) fluid));
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
    public void finish() {
        ActionIterableUtils.multiSetBlockAndUpdate(states, world);
    }

    @Override
    public void fail() {
    }
}
