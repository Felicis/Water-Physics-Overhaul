package net.skds.wpo.mixin.block.withfluidphysics;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.skds.wpo.util.marker.WPOFluidMarker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(value = {FlowingFluidBlock.class})
public abstract class FlowingFluidBlockMixin extends Block implements WPOFluidMarker {
    /**************+ MARKER MIXIN: do not remove this mixing even if empty ****************/

    public FlowingFluidBlockMixin(Properties p_i48440_1_) {
        super(p_i48440_1_);
    }

    /*
         Roadblock
     */
    @Inject(method = "isRandomlyTicking", at = @At(value = "HEAD"))
    private void isRandomlyTicking_roadblock(BlockState pState, CallbackInfoReturnable<Boolean> cir) {
        // nop (fluid is randomly ticked directly -> WorldMixin)
    }

    /*
         Roadblock
     */
    @Inject(method = "randomTick", at = @At(value = "HEAD"))
    private void randomTick_roadblock(BlockState pState, ServerWorld pLevel, BlockPos pPos, Random pRandom, CallbackInfo ci) {
        // nop (fluid is randomly ticked directly -> WorldMixin)
    }


}