package net.skds.wpo.mixin.entity;

import net.minecraft.entity.item.FallingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = { FallingBlockEntity.class })
public class FallingBlockEntityMixin {
	// not needed anymore, because setBlock is redirected and adapts to containing fluid OR displaces (as required here)
//	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlock(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
//	public boolean aaa(World w, BlockPos pos, BlockState newState, int i) {
//		if (!w.isClientSide){
//			ServerWorld world = (ServerWorld) w;
//			newState = FFluidStatic.copyFluidOrEject(world, pos, newState);
//		}
//		return w.setBlock(pos, newState, i);
//	}
}