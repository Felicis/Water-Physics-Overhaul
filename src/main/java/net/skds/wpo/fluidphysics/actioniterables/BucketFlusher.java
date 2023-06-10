package net.skds.wpo.fluidphysics.actioniterables;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.skds.wpo.fluidphysics.FFluidStatic;
import net.skds.wpo.util.Constants;

import static net.minecraft.state.properties.BlockStateProperties.WATERLOGGED;

public class BucketFlusher implements IFluidActionIteratable {

    int maxStateLvl = Constants.MAX_FLUID_LEVEL;
    int bucketLvl;
    boolean complete = false;
    World world;
    FlowingFluid bucketFluid;
    FillBucketEvent event;
    IFluidHandlerItem bucket;
    Long2ObjectLinkedOpenHashMap<BlockState> states = new Long2ObjectLinkedOpenHashMap<>();

    public BucketFlusher(World w, FlowingFluid f, IFluidHandlerItem b, FillBucketEvent e) {
        world = w;
        bucket = b;
        event = e;
        bucketLvl = bucket.getFluidInTank(0).getAmount() / FFluidStatic.MILLIBUCKETS_PER_LEVEL;
        bucketFluid = f;
    }

    @Override
    public boolean isComplete() {
        return complete;
    }

    @Override
    public void run(BlockPos pos, BlockState state) {
        // world.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5,
        // pos.getZ() + 0.5, 0, 0, 0);

        // only full block & not waterlogged yet
        if (false && !state.getValue(WATERLOGGED)) {
            states.clear();
            states.put(pos.asLong(), FFluidStatic.forceApplyFluid(state, maxStateLvl, bucketFluid)); // duplicate liquid!!!!
            complete = true;
            return;
        }

        FluidState fs = state.getFluidState();
        int stateLvl = fs.getAmount();
        int freeBlockLvls = maxStateLvl - stateLvl;
        if (freeBlockLvls <= 0) {
            // block already full
            return;
        } else if (freeBlockLvls < bucketLvl) {
            // empty bucket partially
            int newStateLvl = maxStateLvl;
            bucketLvl -= freeBlockLvls;
            states.put(pos.asLong(), FFluidStatic.forceApplyFluid(state, newStateLvl, bucketFluid));
        } else if (freeBlockLvls >= bucketLvl) {
            // empty bucket fully
            int newStateLvl = stateLvl + bucketLvl;
            bucketLvl = 0;
            states.put(pos.asLong(), FFluidStatic.forceApplyFluid(state, newStateLvl, bucketFluid));
        }

        if (bucketLvl == 0) {
            complete = true;
            return;
        }
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public boolean isValidState(BlockState state) {
        return bucketFluid.isSame(state.getFluidState().getType()) || state.getFluidState().isEmpty();
    }

    @Override
    public void finish() {
        ActionIterableUtils.multiSetBlockAndUpdate(states, world);

        event.setResult(Event.Result.ALLOW);
        PlayerEntity p = event.getPlayer();
        Item item = bucket.getContainer().getItem();
        p.awardStat(Stats.ITEM_USED.get(item));
        SoundEvent soundevent = bucketFluid.getAttributes().getEmptySound();
        if (soundevent == null)
            soundevent = bucketFluid.is(FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA
                    : SoundEvents.BUCKET_EMPTY;
        p.playSound(soundevent, 1.0F, 1.0F);
        if (!p.abilities.instabuild) {
            // bucket.fill(FluidStack.EMPTY, FluidAction.EXECUTE);
            // event.setFilledBucket(bucket.getContainer());
            event.setFilledBucket(new ItemStack(Items.BUCKET));
        }
    }
}
