package net.skds.wpo.fluidphysics.actioniterables;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.skds.wpo.fluidphysics.FFluidStatic;
import net.skds.wpo.util.Constants;
import net.skds.wpo.util.ExtendedFHIS;

import static net.minecraft.state.properties.BlockStateProperties.WATERLOGGED;

public class BucketFiller implements IFluidActionIteratable {

    int bucketLevels = Constants.MAX_FLUID_LEVEL;
    int sl = 0;
    boolean complete = false;
    World world;
    FlowingFluid fluid;
    FillBucketEvent event;
    IFluidHandlerItem bucket;
    Long2ObjectLinkedOpenHashMap<BlockState> states = new Long2ObjectLinkedOpenHashMap<>();

    public BucketFiller(World w, FlowingFluid f, IFluidHandlerItem b, FillBucketEvent e) {
        world = w;
        fluid = f;
        bucket = b;
        event = e;
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
        sl += l;
        int nl = 0;
        if (sl >= bucketLevels) {
            nl = sl - bucketLevels;
            complete = true;
        }
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
    public void fail() {
        // System.out.println(sl);

        ActionIterableUtils.fillStates(states, world);
        event.setResult(Event.Result.ALLOW);
        PlayerEntity p = event.getPlayer();
        Item item = bucket.getContainer().getItem();
        p.awardStat(Stats.ITEM_USED.get(item));
        SoundEvent soundevent = fluid.getAttributes().getFillSound();
        if (soundevent == null)
            soundevent = fluid.is(FluidTags.LAVA) ? SoundEvents.BUCKET_FILL_LAVA
                    : SoundEvents.BUCKET_FILL;
        p.playSound(soundevent, 1.0F, 1.0F);

        if (!p.abilities.instabuild) {
            ItemStack stack = new ItemStack(net.skds.wpo.registry.Items.ADVANCED_BUCKET.get());
            ExtendedFHIS st2 = new ExtendedFHIS(stack, 1000);
            Fluid f2 = fluid instanceof FlowingFluid ? ((FlowingFluid) fluid).getSource() : fluid;
            FluidStack fluidStack = new FluidStack(f2, sl * FFluidStatic.MILLIBUCKETS_PER_LEVEL);
            st2.fill(fluidStack, IFluidHandler.FluidAction.EXECUTE);

            stack = st2.getContainer();
            event.setFilledBucket(stack);
        }
    }

    @Override
    public void finish() {
        ActionIterableUtils.fillStates(states, world);

        event.setResult(Event.Result.ALLOW);
        PlayerEntity p = event.getPlayer();
        Item item = bucket.getContainer().getItem();
        p.awardStat(Stats.ITEM_USED.get(item));
        SoundEvent soundevent = fluid.getAttributes().getFillSound();
        if (soundevent == null)
            soundevent = fluid.is(FluidTags.LAVA) ? SoundEvents.BUCKET_FILL_LAVA
                    : SoundEvents.BUCKET_FILL;
        p.playSound(soundevent, 1.0F, 1.0F);
        if (!p.abilities.instabuild) {
            // bucket.fill(new FluidStack(fluid, 1000), FluidAction.EXECUTE);
            event.setFilledBucket(new ItemStack(fluid.getBucket()));
        }
    }
}
