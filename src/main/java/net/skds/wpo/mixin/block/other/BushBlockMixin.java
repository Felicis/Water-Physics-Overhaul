package net.skds.wpo.mixin.block.other;

import net.minecraft.block.BlockState;
import net.minecraft.block.BushBlock;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BushBlock.class)
public class BushBlockMixin {
    @Inject(method = "propagatesSkylightDown", at = @At(value = "INVOKE", target = "Lnet/minecraft/fluid/FluidState;isEmpty()Z"))
    public void isEmptyFluidState(BlockState pState, IBlockReader pReader, BlockPos pPos, CallbackInfoReturnable<Boolean> cir) {
        pReader.getFluidState(pPos).isEmpty(); // correct FluidState access -> isEmpty
    }

    @Redirect(method = "propagatesSkylightDown", at = @At(value = "INVOKE", target = "Lnet/minecraft/fluid/FluidState;isEmpty()Z"))
    public boolean isEmptyNOP(FluidState instance) {
        return true; // NOP
    }
}
