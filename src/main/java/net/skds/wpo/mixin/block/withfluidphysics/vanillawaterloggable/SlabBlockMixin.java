package net.skds.wpo.mixin.block.withfluidphysics.vanillawaterloggable;

import net.minecraft.block.SlabBlock;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SlabBlock.class)
public class SlabBlockMixin {
    // not needed anymore, because setBlock is redirected and adapts to containing fluid OR displaces (as required here)
//    @Inject(method = "getStateForPlacement", at = @At(value = "RETURN", ordinal = 0),
//            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;is(Lnet/minecraft/block/Block;)Z")),
//            cancellable = true)
//    private void getStateForPlacementDOUBLE(BlockItemUseContext context, CallbackInfoReturnable<BlockState> cir) {
//        // SlabType.DOUBLE was just added and WATERLOGGED was just removed: check if had fluid level > 0 and expel (has fluid level bc is waterlogged)
//        World world = context.getLevel();
//        if (world.isClientSide)
//            return; // only on server
//        BlockPos pos = context.getClickedPos();
//        BlockState oldState = world.getBlockState(pos);
//        BlockState newState = cir.getReturnValue();
//        newState = FFluidStatic.copyFluidOrEject((ServerWorld) world, pos, oldState, newState);
//        cir.setReturnValue(newState); // return new state
//    }
}
