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
import net.skds.wpo.util.tuples.Tuple3;

import java.util.*;
import java.util.stream.Collectors;

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
                TankFiller filler = new TankFiller(world, pos, (FlowingFluid) fluidAtPos, bucketHandler); // safe cast bc not empty
                Tuple2<Boolean, Integer> successAndReturn = filler.tryExecuteWithResult(); // calls setFluid
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
                // if contains different fluid => abort
                event.setCanceled(true);
                return;
            } else { // next block accepts fluid
                // place fluid
                // TODO config max levels to place?
                int oldLevel = bucketHandler.getFluidInTank(0).getAmount() / Constants.MILLIBUCKETS_PER_LEVEL;
                TankFlusher flusher = new TankFlusher(world, pos, (FlowingFluid) bucketFluid, oldLevel); // safe cast because not empty
                Tuple2<Boolean, Integer> successAndNewLevel = flusher.tryExecuteWithResult(); // calls setFluid
                int newLevel = successAndNewLevel.second;
                if (oldLevel == newLevel) { // nothing was placed => abort
                    event.setCanceled(true);
                    return;
                } else { // fluid was placed
                    if (!player.abilities.instabuild) { // if NOT creative mode => set new bucket level
                        if (successAndNewLevel.first) { // bucket was emptied completely
                            event.setFilledBucket(new ItemStack(Items.BUCKET)); // empty bucket
                        } else { // partially filled bucket (WPO advanced bucket)
                            // TODO handle forge containers
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
    }

    public static boolean onBottleUse(World w, BlockPos pos) {
        BottleFiller bottleFiller = new BottleFiller(w, pos, Fluids.WATER);
        return bottleFiller.tryExecute();
    }

    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        /* accumulate special cases here (modify FS and BS as needed - consider that setBlock is called later) */
        /* (if for some reason block placement should not be allowed, cancel event) */
        World world = (World) event.getWorld();
        BlockPos pos = event.getPos();
        BlockState blockState = event.getPlacedBlock();
        Block newBlock = blockState.getBlock();
        // TODO handle sponge block
        if (newBlock instanceof SpongeBlock || newBlock instanceof WetSpongeBlock) {
            return;
        }
        // frost walker replaces water with water (idk why) => delete water (since it is created again from melting ice)
        // idk when FrostedIceBlock is placed...
        int frostWalkerLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.FROST_WALKER, (LivingEntity) event.getEntity());
        if (newBlock == Blocks.WATER && frostWalkerLevel > 0) {
            // does not duplicate water, because frostwalker only triggers on source blocks and the ice melts to 8 levels
            ((WorldMixinInterface) world).setFluid(pos, Fluids.EMPTY.defaultFluidState()); // remove water (is ice now)
            return;
        }
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

        boolean isExtending = event.getPistonMoveType().isExtend;
        Direction pistonFacingDirection = event.getDirection();
        Direction moveDirection = isExtending ? pistonFacingDirection : pistonFacingDirection.getOpposite();
        BlockPos pistonBasePos = event.getPos();
        BlockPos pistonHeadPos = pistonBasePos.relative(moveDirection);
        List<BlockPos> destroyed = ps.getToDestroy(); // pos that are destroyed by piston (e.g. flower) -- air does not need to be 'destroyed'
        List<BlockPos> toPush = ps.getToPush(); // contains pos left empty after push/pull (e.g.  of wooden planks, sticky, ...) => creates air if not push destination
        List<BlockPos> pushedTo = ps.getToPush().stream().map(p -> p.relative(moveDirection)).collect(Collectors.toList()); // push destinations
        List<BlockPos> vacated = toPush.stream().filter(pos -> !pushedTo.contains(pos)).collect(Collectors.toList()); // toPush && !pushedTo: left empty after push
        // TODO if extending => remove pistonhead from vacated
        // cache old states
        Set<BlockPos> allModified = new HashSet<>(destroyed);
        allModified.addAll(toPush);
        allModified.addAll(pushedTo);
        Map<BlockPos, FluidState> oldStates = new HashMap<>();
        for (BlockPos pos : allModified) {
            oldStates.put(pos, world.getFluidState(pos));
        }

        // 1st) copy pushed fluids to destination
        for (BlockPos oldPos : toPush) {
            BlockPos newPos = oldPos.relative(moveDirection); // after moving
            ((WorldMixinInterface) world).setFluid(newPos, oldStates.get(oldPos), 16 | 2);
        }

        // destroy piston head fluids if extending
        if (isExtending) {
            ((WorldMixinInterface) world).setFluid(pistonHeadPos, Fluids.EMPTY.defaultFluidState(), 16 | 2);
        }

        // 2nd) empty vacated pos's (were already copied in 1st)
        for (BlockPos pos : vacated) {
            ((WorldMixinInterface) world).setFluid(pos, Fluids.EMPTY.defaultFluidState(), 16 | 2);
        }

        // collect invalid pos's for displacer: destroyed, pushedTo, piston (implicitly valid: vacated)
        Set<BlockPos> invalidPosSet = new HashSet<>(destroyed);
        invalidPosSet.addAll(pushedTo);
        invalidPosSet.add(pistonBasePos); // piston base pos (never allowed)
        if (isExtending) { // when extending: piston head not in pushedTo or destroyed, but not valid
            invalidPosSet.add(pistonHeadPos); // piston head pos (destination)
            // TODO IDEA: fluidlog piston head? => double pump: need to process extending and retracting separately & set head pos as valid
        } // when contracting: piston head in pushedTo (sticky/slime = blocked) or becomes empty (=valid) => no handling needed

        // 3rd) displace all fluids in pos that will be destroyed (e.g. ladder, fluid block)
        for (BlockPos pos : destroyed) {
            FluidState displacedFS = oldStates.get(pos);
            if (!displacedFS.isEmpty()) { // not empty => displace levels
                FlowingFluid fluidToDisplace = (FlowingFluid) displacedFS.getType(); // safe cast because not empty
                int levelsToDisplace = displacedFS.getAmount();
                // 1) place as many levels as possible into pushed block (the block which moved into this pos forcing the displacing)
                FluidState pushedFluidState = world.getFluidState(pos);
                BlockState pushedBlockState = world.getBlockState(pos.relative(moveDirection.getOpposite())); // pushing not happened yet!!!
                if (FFluidStatic.canHoldFluid(pushedBlockState) && FFluidStatic.canFlow(world, pos, moveDirection.getOpposite())
                        && (fluidToDisplace.isSame(pushedFluidState.getType()) || pushedFluidState.isEmpty())) {
                    Tuple3<Boolean, Integer, FluidState> tuple3 = FFluidStatic.placeLevelsUpTo(pushedFluidState, fluidToDisplace, levelsToDisplace);
                    Boolean wasPlaced = tuple3.first;
                    Integer placedLevels = tuple3.second;
                    FluidState newPushedFluidState = tuple3.third;
                    if (wasPlaced) { // levels were actually placed
                        levelsToDisplace -= placedLevels;
                        FFluidStatic.setFluidAlsoBlock(world, pos, newPushedFluidState); // update pushed block
                        if (levelsToDisplace == 0) { // everything placed, nothing left to displace
                            return;
                        }
                    }
                }
                // 2) displace remaining levels with piston displacer
                PistonDisplacer displacer = new PistonDisplacer(world, pos, fluidToDisplace, levelsToDisplace, invalidPosSet);
                if (!displacer.tryExecute()) { // if cannot displace fluid
                    event.setCanceled(true); // do not destroy fluid with piston! (alternatively destroy/void by not canceling)
                    return; // stop checking other pos's
                }
            }
        }
        event.setResult(Event.Result.ALLOW);
    }
}
