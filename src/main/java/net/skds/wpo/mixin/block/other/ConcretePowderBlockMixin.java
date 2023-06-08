package net.skds.wpo.mixin.block.other;

import net.minecraft.block.BlockState;
import net.minecraft.block.ConcretePowderBlock;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ConcretePowderBlock.class)
public class ConcretePowderBlockMixin {
    @Redirect(method = "shouldSolidify", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/ConcretePowderBlock;canSolidify(Lnet/minecraft/block/BlockState;)Z"))
    private static boolean canSolidifyM(BlockState pState, IBlockReader pLevel, BlockPos pPos) {
        return canSolidify(pLevel, pPos); // use custom method
    }

    /*
     * Roadblock Injection
     */
    @Inject(method = "touchesLiquid", at = @At(value = "HEAD"), cancellable = true)
    private static void touchesLiquidM(IBlockReader pLevel, BlockPos pPos, CallbackInfoReturnable<Boolean> cir) {
        // complete copy of code with 2 changes
        boolean flag = false;
        BlockPos.Mutable blockpos$mutable = pPos.mutable();

        for (Direction direction : Direction.values()) {
            BlockState blockstate = pLevel.getBlockState(blockpos$mutable);
            if (direction != Direction.DOWN || canSolidify(pLevel, blockpos$mutable)) { // new canSolidify method
                blockpos$mutable.setWithOffset(pPos, direction);
                blockstate = pLevel.getBlockState(blockpos$mutable);
                if (canSolidify(pLevel, blockpos$mutable) && !blockstate.isFaceSturdy(pLevel, pPos, direction.getOpposite())) { // new canSolidify method
                    flag = true;
                    break;
                }
            }
        }

        cir.setReturnValue(flag);
    }

    private static boolean canSolidify(IBlockReader pLevel, BlockPos pPos) {
        return pLevel.getFluidState(pPos).is(FluidTags.WATER);
    }
}
