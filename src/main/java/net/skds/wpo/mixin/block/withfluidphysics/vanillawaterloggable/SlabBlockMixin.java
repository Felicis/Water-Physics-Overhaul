package net.skds.wpo.mixin.block.withfluidphysics.vanillawaterloggable;

import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.skds.wpo.fluidphysics.FFluidStatic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SlabBlock.class)
public class SlabBlockMixin {
    @Inject(method = "getStateForPlacement", at = @At(value = "RETURN", ordinal = 0),
            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;is(Lnet/minecraft/block/Block;)Z")),
            cancellable = true)
    private void getStateForPlacementDOUBLE(BlockItemUseContext context, CallbackInfoReturnable<BlockState> cir) {
        // SlabType.DOUBLE was just added and WATERLOGGED was just removed: check if had fluid level > 0 and expel (has fluid level bc is waterlogged)
        World world = context.getLevel();
        if (world.isClientSide)
            return; // only on server
        BlockPos pos = context.getClickedPos();
        BlockState oldState = world.getBlockState(pos);
        BlockState newState = cir.getReturnValue();
        newState = FFluidStatic.copyFluidOrEject((ServerWorld) world, pos, oldState, newState);
        cir.setReturnValue(newState); // return new state
    }
}
