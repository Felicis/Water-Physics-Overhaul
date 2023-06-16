package net.skds.wpo.mixin.item;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.skds.wpo.fluidphysics.EventStatic;
import net.skds.wpo.fluidphysics.RenderStatic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.GlassBottleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RayTraceContext.FluidMode;
import net.minecraft.world.World;

@Mixin(value = { GlassBottleItem.class })
public class GlassBottleItemMixin {

	@ModifyArg(method = "Lnet/minecraft/item/GlassBottleItem;use(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/GlassBottleItem;getPlayerPOVHitResult(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/math/RayTraceContext$FluidMode;)Lnet/minecraft/util/math/BlockRayTraceResult;", args = "Lnet/minecraft/util/math/RayTraceContext$FluidMode;"))
	public FluidMode aaa(FluidMode fm) {
		return FluidMode.ANY; // allow Fluid targeting (RayTraceContext)

	}

	@Inject(method = "Lnet/minecraft/item/GlassBottleItem;use(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/util/SoundEvent;Lnet/minecraft/util/SoundCategory;FF)V", ordinal = 1), cancellable = true)
	public void bbb(World w, PlayerEntity p, Hand hand, CallbackInfoReturnable<ActionResult<ItemStack>> ci) {
		// inject to only change fluid level where water was collected (water bottle created and returned after mixin)
		// only allows Water pickup!! (else mixin before fluid type check & add other fluid bottles)
		BlockRayTraceResult rt = RenderStatic.rayTrace(w, p, RayTraceContext.FluidMode.ANY);
		BlockPos pos = rt.getBlockPos();
		boolean success = EventStatic.onBottleUse(w, pos); // try slurping water for bottle from this pos (return success)
		if (success) {
			return;
		} else {
			ci.setReturnValue(ActionResult.pass(p.getItemInHand(hand)));
		}
	}
}