package net.skds.wpo.fluidphysics.actioniterables;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.skds.wpo.fluidphysics.FFluidStatic;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.minecraft.state.properties.BlockStateProperties.WATERLOGGED;

public class BottleFiller implements IFluidActionIteratable {

    int bucketLevels = 3;
    int sl = 0;
    boolean complete = false;
    World world;
    ItemStack bottle;
    FlowingFluid fluid;
    CallbackInfoReturnable<ActionResult<ItemStack>> ci;
    Long2ObjectLinkedOpenHashMap<BlockState> states = new Long2ObjectLinkedOpenHashMap<>();

    public BottleFiller(World w, FlowingFluid f, CallbackInfoReturnable<ActionResult<ItemStack>> ci, ItemStack stack) {
        world = w;
        fluid = f;
        bottle = stack;
        this.ci = ci;
    }

    @Override
    public boolean isComplete() {
        return complete;
    }

    @Override
    public void run(BlockPos pos, BlockState state) {
        // world.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5,
        // pos.getZ() + 0.5, 0, 0, 0);

        if (false && state.getValue(WATERLOGGED)) {
            states.clear();
            states.put(pos.asLong(), FFluidStatic.forceApplyFluid(state, 0, fluid));
            complete = true;
            return;
        }
        FluidState fs = state.getFluidState();
        int l = fs.getAmount();
        int osl = sl;
        sl += l;
        int nl = 0;
        if (sl >= bucketLevels) {
            nl = sl - bucketLevels;
            complete = true;
        }
        if (osl != sl)
            states.put(pos.asLong(), FFluidStatic.forceApplyFluid(state, nl, fluid));
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public boolean isValidState(BlockState state) {
        return fluid.isSame(state.getFluidState().getType());
    }

    @Override
    public void finish() {
        ActionIterableUtils.fillStates(states, world);
    }

    @Override
    public void fail() {
        ci.setReturnValue(ActionResult.fail(bottle));
    }
}
