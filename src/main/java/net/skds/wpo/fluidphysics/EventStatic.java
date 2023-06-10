package net.skds.wpo.fluidphysics;

import net.minecraft.block.*;
import net.minecraft.block.material.Material;
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
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.PistonEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.skds.wpo.WPOConfig;
import net.skds.wpo.fluidphysics.actioniterables.*;
import net.skds.wpo.util.ExtendedFHIS;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import virtuoel.statement.util.RegistryUtils;

import java.util.List;
import java.util.Optional;

import static net.minecraft.state.properties.BlockStateProperties.WATERLOGGED;

public class EventStatic {
    public static void onBucketEvent(FillBucketEvent e) {

        FishBucketItem fishItem = null;
        ItemStack bucket = e.getEmptyBucket();
        Item bu = bucket.getItem();
        if (bu instanceof FishBucketItem) {
            fishItem = (FishBucketItem) bu;
        }
        Optional<IFluidHandlerItem> op = bucket.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY)
                .resolve();
        IFluidHandlerItem bh;
        if (op.isPresent()) {
            bh = op.get();
        } else { // WPO advanced bucket
            bh = new ExtendedFHIS(bucket, 1000);
            // System.out.println("l;hhhhhhh " + bh);
        }
        Fluid bucketFluid = bh.getFluidInTank(0).getFluid();
        if (!(bucketFluid instanceof FlowingFluid) && bucketFluid != Fluids.EMPTY) {
            return;
        }
        PlayerEntity p = e.getPlayer();
        World w = e.getWorld();
        BlockRayTraceResult targ = rayTrace(w, p,
                bucketFluid == Fluids.EMPTY ? RayTraceContext.FluidMode.ANY : RayTraceContext.FluidMode.NONE);
        if (targ.getType() != RayTraceResult.Type.BLOCK) {
            return;
        }
        BlockPos pos = targ.getBlockPos();
        BlockState bs = w.getBlockState(pos);
        FluidState fs = bs.getFluidState();
        Fluid blockFluid = fs.getType();
        // if target block not interactable OR item not usable on target block
        if (!(w.mayInteract(p, pos) && p.mayUseItemAt(pos, targ.getDirection(), bh.getContainer()))) {
            return;
        }

        if (bucketFluid == Fluids.EMPTY) { // PICK UP
            if (blockFluid == Fluids.EMPTY) {
                return; // nothing to pick up
            }
            // pick up
            BucketFiller filler = new BucketFiller(w, (FlowingFluid) blockFluid, bh, e);
            FFluidStatic.iterateFluidWay(WPOConfig.COMMON.maxBucketDist.get(), pos, filler); // why not equalize greater area?
        } else { // PLACE
            // if block cannot accept water => try next block towards player (e.g. place water against wall)
            if (fs.isEmpty() && !bs.hasProperty(WATERLOGGED)) {
                pos = pos.relative(targ.getDirection());
                bs = w.getBlockState(pos);
            }
            if (!w.isClientSide && bs.hasProperty(WATERLOGGED)) {
                FluidTasksManager.addFluidTask((ServerWorld) w, pos);
            }
            if (!bucketFluid.isSame(blockFluid) && blockFluid != Fluids.EMPTY) {
                e.setCanceled(true); // cannot place fluid into a different fluid
                return;
            }
            // place fluid
            BucketFlusher flusher = new BucketFlusher(w, (FlowingFluid) bucketFluid, bh, e);
//            if (FFluidStatic.iterateFluidWay(WPOConfig.COMMON.maxBucketDist.get(), pos, flusher) && fishItem != null) {
//                fishItem.checkExtraContent(w, bucket, pos);
//            }
        }
    }

    public static void onBottleUse(World w, PlayerEntity p, CallbackInfoReturnable<ActionResult<ItemStack>> ci, ItemStack stack) {
        BlockRayTraceResult rt = rayTrace(w, p, RayTraceContext.FluidMode.ANY);
        BlockPos pos = rt.getBlockPos();

        BottleFiller filler = new BottleFiller(w, Fluids.WATER, ci, stack);
        FFluidStatic.iterateFluidWay(WPOConfig.COMMON.maxBucketDist.get(), pos, filler);
    }

    public static void onBlockPlace(BlockEvent.EntityPlaceEvent e) {
        World w = (World) e.getWorld();
        if (!w.isClientSide) { // only server side
            ServerWorld world = (ServerWorld) w;
            BlockPos pos = e.getPos();
            BlockState oldState = e.getBlockSnapshot().getReplacedBlock();
            FluidState oldFluidState = oldState.getFluidState();
            BlockState newState = e.getPlacedBlock();
            Block newBlock = newState.getBlock();
            if (newBlock instanceof SpongeBlock || newBlock instanceof WetSpongeBlock) {
                return;
            }
            // frost walker replaces water with water (idk why) => delete water (since it is created again from melting ice)
            // idk when FrostedIceBlock is placed...
            int frostWalkerLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.FROST_WALKER, (LivingEntity) e.getEntity());
            if (frostWalkerLevel > 0 && newBlock == Blocks.WATER) {
                return; // let the water be silently replaced
                // does not duplicate water, because frostwalker only triggers on source blocks and the ice melts to a source block
            }
            // default: try to apply, else eject
            newState = FFluidStatic.copyFluidOrEject(world, pos, oldState, newState);
            w.setBlockAndUpdate(pos, newState);
            // BlockEvent.EntityPlaceEvent does not have a result
        }
    }

    public static void onPistonPre(PistonEvent.Pre e) {
        World w = (World) e.getWorld();
        if (w.isClientSide || e.isCanceled()) {
            return;
        }
        PistonBlockStructureHelper ps = e.getStructureHelper();

        if (!ps.resolve()) {
            return;
        }
        List<BlockPos> poslist = ps.getToDestroy();

        for (BlockPos pos : poslist) {
            BlockState state = w.getBlockState(pos);
            FluidState fs = state.getFluidState();
            // System.out.println(state);

            if (!fs.isEmpty()) {

                // System.out.println("jjj");

                PistonDisplacer displacer = new PistonDisplacer(w, e, state, ps);
                if (!FFluidStatic.iterateFluidWay(12, pos, displacer)) {
                    e.setCanceled(true);
                }
            }
        }
    }

    public static BlockRayTraceResult rayTrace(World worldIn, PlayerEntity player,
                                               RayTraceContext.FluidMode fluidMode) {
        float f = player.xRot;
        float f1 = player.yRot;
        Vector3d vector3d = player.getEyePosition(1.0F);
        float f2 = MathHelper.cos(-f1 * ((float) Math.PI / 180F) - (float) Math.PI);
        float f3 = MathHelper.sin(-f1 * ((float) Math.PI / 180F) - (float) Math.PI);
        float f4 = -MathHelper.cos(-f * ((float) Math.PI / 180F));
        float f5 = MathHelper.sin(-f * ((float) Math.PI / 180F));
        float f6 = f3 * f4;
        float f7 = f2 * f4;
        double d0 = player.getAttribute(net.minecraftforge.common.ForgeMod.REACH_DISTANCE.get()).getValue();
        Vector3d vector3d1 = vector3d.add((double) f6 * d0, (double) f5 * d0, (double) f7 * d0);
        return worldIn.clip(
                new RayTraceContext(vector3d, vector3d1, RayTraceContext.BlockMode.OUTLINE, fluidMode, player));
    }
}
