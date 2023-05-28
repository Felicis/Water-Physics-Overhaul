package net.skds.wpo.mixin.entity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.item.FallingBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.skds.wpo.fluidphysics.FFluidStatic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = { FallingBlockEntity.class })
public class FallingBlockEntityMixin {

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlock(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
	public boolean aaa(World w, BlockPos pos, BlockState newState, int i) {
		if (!w.isClientSide){
			ServerWorld world = (ServerWorld) w;
			newState = FFluidStatic.copyFluidOrEject(world, pos, newState);
		}
		return w.setBlock(pos, newState, i);
	}
}