package net.skds.wpo.fluidphysics;

import net.minecraft.block.*;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.FishBucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.PistonEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.skds.wpo.fluidphysics.actioniterables.BottleFiller;
import net.skds.wpo.fluidphysics.actioniterables.PistonDisplacer;
import net.skds.wpo.fluidphysics.actioniterables.TankFiller;
import net.skds.wpo.fluidphysics.actioniterables.TankFlusher;
import net.skds.wpo.item.AdvancedBucket;
import net.skds.wpo.mixininterfaces.WorldMixinInterface;
import net.skds.wpo.util.Constants;
import net.skds.wpo.util.tuples.Tuple2;

import static net.skds.wpo.registry.Items.ADVANCED_BUCKET;

public class EventStatic {
    public static void onBucketEvent(FillBucketEvent event) {
        // get from event
        PlayerEntity player = event.getPlayer();
        World world = event.getWorld();
        ItemStack bucketItemStack = event.getEmptyBucket(); // current bucket item stack
        // get fluid handler (forge or advanced bucket)
        IFluidHandlerItem bucketHandler = bucketItemStack
                .getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY)
                .resolve()
                .orElseGet(() -> new AdvancedBucket.FluidHandler(bucketItemStack));
        // get fluid raytrace hit (event raytrace only targets blocks)
        Fluid bucketFluid = bucketHandler.getFluidInTank(0).getFluid();
        if (!(bucketFluid instanceof FlowingFluid) && bucketFluid != Fluids.EMPTY) {
            return; // why needed? are there not flowing fluids except empty?
        }
        BlockRayTraceResult targ = RenderStatic.rayTrace(world, player,
                bucketFluid == Fluids.EMPTY ? RayTraceContext.FluidMode.ANY : RayTraceContext.FluidMode.NONE);
        if (targ.getType() != RayTraceResult.Type.BLOCK) { // if MISS (click in air)
            return;
        }
        // get (raytrace) target
        BlockPos pos = targ.getBlockPos();
        BlockState blockState = world.getBlockState(pos);
        FluidState fluidState = world.getFluidState(pos);
        Fluid fluidAtPos = fluidState.getType();
        // if target block not interactable OR item not usable on target block
        if (!(world.mayInteract(player, pos) && player.mayUseItemAt(pos, targ.getDirection(), bucketHandler.getContainer()))) {
            return;
        }

        if (bucketFluid.isSame(Fluids.EMPTY)) { // PICK UP (bucket empty)
            if (!fluidAtPos.isSame(Fluids.EMPTY)) { // there is fluid at target pos
                TankFiller filler = new TankFiller(world, (FlowingFluid) fluidAtPos, bucketHandler); // safe cast bc not empty
                Tuple2<Boolean, Integer> successAndReturn = filler.tryExecuteWithResult(pos); // calls setFluid
                if (!player.abilities.instabuild) { // if NOT creative mode => set new bucket level
                    if (successAndReturn.first) { // bucket was filled completely
                        event.setFilledBucket(new ItemStack(fluidAtPos.getBucket())); // full bucket
                    } else { // partially filled bucket (WPO advanced bucket)
                        // TODO handle forge containers
                        int newLevel = successAndReturn.second;
                        AdvancedBucket.FluidHandler advBucketHandler = new AdvancedBucket.FluidHandler(new ItemStack(ADVANCED_BUCKET.get()));
                        FluidStack fluidStack = new FluidStack(fluidAtPos, newLevel * Constants.MILLIBUCKETS_PER_LEVEL);
                        advBucketHandler.fill(fluidStack, IFluidHandler.FluidAction.EXECUTE);
                        event.setFilledBucket(advBucketHandler.getContainer());
                    }
                }
                // skip vanilla code: forge reduces bucket stack and places filled bucket from event in inventory
                event.setResult(Event.Result.ALLOW);
                // award stats & play sound (not done by forge; see BucketItem and ForgeEventFactory)
                Item item = bucketHandler.getContainer().getItem();
                player.awardStat(Stats.ITEM_USED.get(item));
                SoundEvent soundevent = fluidAtPos.getAttributes().getFillSound();
                if (soundevent == null) {
                    soundevent = fluidAtPos.is(FluidTags.LAVA) ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL;
                }
                player.playSound(soundevent, 1.0F, 1.0F);
                return;
            } else { // no fluid at target pos
                // nop: could also abort, but no difference for vanilla (modded?)
            }
        } else { // PLACE (bucket contains fluid)
            // if block cannot accept fluid => try next block towards player (e.g. place water against wall)
            if (!FFluidStatic.canHoldFluid(blockState)) {
                pos = pos.relative(targ.getDirection()); // update pos
                blockState = world.getBlockState(pos);
                fluidState = world.getFluidState(pos);
                fluidAtPos = fluidState.getType();
                // if next block does not work either => abort
                if (!FFluidStatic.canHoldFluid(blockState)) {
                    event.setCanceled(true);
                    return;
                }
            }
            if (!fluidAtPos.isSame(Fluids.EMPTY) && !fluidAtPos.isSame(bucketFluid)) {
                // if pos contains different fluid => abort
                event.setCanceled(true);
                return;
            } else { // next block accepts fluid
                // place fluid
                TankFlusher flusher = new TankFlusher(world, bucketHandler);
                Tuple2<Boolean, Integer> successAndReturn = flusher.tryExecuteWithResult(pos); // calls setFluid
                if (!player.abilities.instabuild) { // if NOT creative mode => set new bucket level
                    if (successAndReturn.first) { // bucket was emptied completely
                        event.setFilledBucket(new ItemStack(Items.BUCKET)); // empty bucket
                    } else { // partially filled bucket (WPO advanced bucket)
                        // TODO handle forge containers
                        int newLevel = successAndReturn.second;
                        AdvancedBucket.FluidHandler advBucketHandler = new AdvancedBucket.FluidHandler(new ItemStack(ADVANCED_BUCKET.get()));
                        FluidStack fluidStack = new FluidStack(fluidAtPos, newLevel * Constants.MILLIBUCKETS_PER_LEVEL);
                        advBucketHandler.fill(fluidStack, IFluidHandler.FluidAction.EXECUTE);
                        event.setFilledBucket(advBucketHandler.getContainer());
                    }
                }
                // skip vanilla code: forge reduces bucket stack and places filled bucket from event in inventory
                event.setResult(Event.Result.ALLOW);
                // award stats & play sound (not done by forge; see BucketItem and ForgeEventFactory)
                Item item = bucketHandler.getContainer().getItem();
                player.awardStat(Stats.ITEM_USED.get(item));
                SoundEvent soundevent = fluidAtPos.getAttributes().getEmptySound();
                if (soundevent == null) {
                    soundevent = fluidAtPos.is(FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY;
                }
                player.playSound(soundevent, 1.0F, 1.0F);
            }
            Item bucketItem = bucketItemStack.getItem();
            if (bucketItem instanceof FishBucketItem) {
                FishBucketItem fishItem = (FishBucketItem) bucketItem;
                fishItem.checkExtraContent(world, bucketItemStack, pos);
            }
        }
    }

    public static boolean onBottleUse(World w, BlockPos pos) {
        BottleFiller bottleFiller = new BottleFiller(w, Fluids.WATER);
        return bottleFiller.tryExecute(pos);
    }

    public static void onBlockPlace(BlockEvent.EntityPlaceEvent e) {
        World world = (World) e.getWorld();
        BlockPos pos = e.getPos();
        // get block and fluid (overwrite to change and break)
        FluidState fluidState = world.getFluidState(pos);
        BlockState blockState = e.getPlacedBlock();
        Block newBlock = blockState.getBlock();
        boolean once = true;
        while (once) { // only to skip other checks easily (like case, but heterogeneous checks)
            once = false;
            /* accumulate special cases here (modify FS and BS as needed and break) */
            if (newBlock instanceof SpongeBlock || newBlock instanceof WetSpongeBlock) {
                break;
            }
            // frost walker replaces water with water (idk why) => delete water (since it is created again from melting ice)
            // idk when FrostedIceBlock is placed...
            int frostWalkerLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.FROST_WALKER, (LivingEntity) e.getEntity());
            if (newBlock == Blocks.WATER && frostWalkerLevel > 0) {
                // does not duplicate water, because frostwalker only triggers on source blocks and the ice melts to 8 levels
                fluidState = Fluids.EMPTY.defaultFluidState(); // remove water (is ice now)
                break;
            }
        }
        // set block and fluid
        FFluidStatic.setBlockAndFluid(world, pos, blockState, fluidState, true); // set block and fluid and displace as needed
        // BlockEvent.EntityPlaceEvent does not have a result
    }

    public static void onPistonPre(PistonEvent.Pre event) {
        World world = (World) event.getWorld();
        if (world.isClientSide || event.isCanceled()) { // TODO check: only displace fluids in server (more efficient?)
            return;
        }
        PistonBlockStructureHelper ps = event.getStructureHelper();
        if (ps == null || !ps.resolve()) { // check whether piston can move (e.g. not blocked by obsidian)
            return;
        }

        // 1st) move fluids with blocks, that are pushed (so the fluids are safe from displaced fluids)
        Direction pistonFacingDirection = event.getDirection();
        boolean isExtending = event.getPistonMoveType().isExtend;
        Direction moveDirection = isExtending ? pistonFacingDirection : pistonFacingDirection.getOpposite();
        for (BlockPos oldPos : ps.getToPush()) {
            FluidState fluidState = world.getFluidState(oldPos);
            BlockPos newPos = oldPos.relative(moveDirection); // after moving
            // move fluid without adapting to block, because blocks are moving (Blocks.MOVING_PISTON) and invalid for checks.
            // adapt,  blockupdate (flag=1) and neighbor check later, when moving block stops moving (PistonTileEntity.finishTick)
            ((WorldMixinInterface) world).setFluid(oldPos, Fluids.EMPTY.defaultFluidState(), 16 | 2); // remove old
            ((WorldMixinInterface) world).setFluid(newPos, fluidState, 16 | 2); // add new
        }

        // 2nd) displace all fluids in pos that will be destroyed (e.g. ladder, fluid block)
        for (BlockPos pos : ps.getToDestroy()) {
            FluidState fluidState = world.getFluidState(pos);
            if (!fluidState.isEmpty()) {
                BlockPos pistonBasePos = event.getPos();
                PistonDisplacer displacer = new PistonDisplacer(world, pistonBasePos, pistonFacingDirection, isExtending, fluidState, ps);
                if (!displacer.tryExecute(pos)) { // if cannot displace fluid
                    event.setCanceled(true); // do not destroy fluid with piston! (alternatively destroy/void by not canceling)
                    return; // stop checking other pos's
                }
            }
        }
        event.setResult(Event.Result.ALLOW);
    }
}
