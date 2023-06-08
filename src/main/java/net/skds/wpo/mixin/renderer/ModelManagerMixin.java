package net.skds.wpo.mixin.renderer;

import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.ModelManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ModelManager.class)
public class ModelManagerMixin {
    @Inject(method = "requiresRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;getFluidState()Lnet/minecraft/fluid/FluidState;"), cancellable = true)
    private void skipFluidStateCheck(BlockState pOldState, BlockState pNewState, CallbackInfoReturnable<Boolean> cir){
        // UPGRADE (optional?): this redirects BlockState.getFluidState (!) and returns false
        // (fluid state equality check done in ClientWorld.setFluidsDirty, which is on the chain calling this)
        cir.setReturnValue(false); // BlockState.getFluidState -> NOP
    }
}
