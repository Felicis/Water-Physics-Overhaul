package net.skds.wpo.mixin.fluid;

import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.WaterFluid;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.skds.wpo.fluidphysics.FluidStatic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(value = {WaterFluid.class})
public class WaterFluidMixin {

    @Inject(method = "getTickDelay", at = @At(value = "TAIL"), cancellable = true)
    public void getTickDelayM(CallbackInfoReturnable<Integer> ci) {
        Integer oldTickDelay = ci.getReturnValue();
        int newTickDelay = FluidStatic.getTickDelay(oldTickDelay);
        ci.setReturnValue(newTickDelay);
    }

    /*
        Roadblock
     */
    @Inject(method = "animateTick", at = @At(value = "HEAD"), cancellable = true)
    private void animateTickM(World pLevel, BlockPos pPos, FluidState pState, Random pRandom, CallbackInfo ci) {
        // add particles for all water (not only source), corrected for level
        // no sounds!!! (add those when flowing)
        if (pRandom.nextInt(10) == 0) { // source or flowing
            double yPosParticle = (double) pPos.getY() + pRandom.nextDouble() * pState.getOwnHeight(); // CHANGE: no underwater particles above surface
            pLevel.addParticle(ParticleTypes.UNDERWATER, (double) pPos.getX() + pRandom.nextDouble(), yPosParticle, (double) pPos.getZ() + pRandom.nextDouble(), 0.0D, 0.0D, 0.0D);
        }
        ci.cancel();
    }
}