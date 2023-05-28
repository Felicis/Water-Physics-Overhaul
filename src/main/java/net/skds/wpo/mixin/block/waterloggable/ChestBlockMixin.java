package net.skds.wpo.mixin.block.waterloggable;

import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.fluid.FluidState;
import net.skds.wpo.fluidphysics.FFluidStatic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChestBlock.class)
public class ChestBlockMixin {
    /**
     * RoadBlock Injection to catch FluidState creation
     */
    @Inject(method = "getFluidState", at = @At(value = "HEAD"), cancellable = true)
    public void getFluidStateM(BlockState state, CallbackInfoReturnable<FluidState> cir) {
        cir.setReturnValue(FFluidStatic.getFluidState(state));
    }
}
