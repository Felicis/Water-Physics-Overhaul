package net.skds.wpo.mixin.fluid;

import net.minecraft.fluid.LavaFluid;
import net.skds.wpo.fluidphysics.FluidStatic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LavaFluid.class)
public class LavaFluidMixin {
    @Inject(method = "getTickDelay", at = @At(value = "TAIL"), cancellable = true)
    public void getTickDelayM(CallbackInfoReturnable<Integer> ci) {
        Integer oldTickDelay = ci.getReturnValue();
        int newTickDelay = FluidStatic.getTickDelay(oldTickDelay);
        ci.setReturnValue(newTickDelay);
    }
}
