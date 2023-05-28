package net.skds.wpo.mixin.fluid;

import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.skds.wpo.fluidphysics.FFluidStatic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ForgeFlowingFluid.class)
public class ForgeFlowingFluidMixin {
    @Inject(method = "getTickDelay", at = @At(value = "TAIL"), cancellable = true)
    public void getTickDelayM(CallbackInfoReturnable<Integer> ci) {
        Integer oldTickDelay = ci.getReturnValue();
        int newTickDelay = FFluidStatic.getTickDelay(oldTickDelay);
        ci.setReturnValue(newTickDelay);
    }
}
