package net.skds.wpo.mixin.block.withfluidphysics;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.skds.wpo.fluidphysics.FFluidStatic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(value = {FlowingFluidBlock.class})
public abstract class FlowingFluidBlockMixin extends Block {
    @Shadow
    @Final
    private FlowingFluid fluid;

    public FlowingFluidBlockMixin(Properties p_i48440_1_) {
        super(p_i48440_1_);
    }

    /*
         Roadblock
     */
    @Inject(method = "isRandomlyTicking", at = @At(value = "HEAD"), cancellable = true)
    private void isRandomlyTicking_roadblock(BlockState pState, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
        // nop (fluid is randomly ticked directly -> WorldMixin)
    }

    /*
         Roadblock
     */
    @Inject(method = "randomTick", at = @At(value = "HEAD"), cancellable = true)
    private void randomTick_roadblock(BlockState pState, ServerWorld pLevel, BlockPos pPos, Random pRandom, CallbackInfo ci) {
        ci.cancel();
        // nop (fluid is randomly ticked directly -> WorldMixin)
    }

    // getFluidState: fixedin in AbstractBlockState.getFluidState -> calls this

    // skipRendering: fixed in Block.shouldRenderFace -> calls BlockState.skipRendering & FluidState.skipRendering

    /*
         Roadblock
     */
    @Inject(method = "onPlace", at = @At(value = "HEAD"), cancellable = true)
    private void onPlaceM(BlockState pState, World pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving, CallbackInfo ci) {
        // TODO sync with FluidMixin
        ci.cancel(); // do not schedule fluid tick here. setFluid already does it
    }

    /*
        Roadblock
     */
    @Inject(method = "updateShape", at = @At(value = "HEAD"), cancellable = true)
    private void updateShapeM(BlockState pState, Direction pFacing, BlockState pFacingState, IWorld pLevel, BlockPos pCurrentPos, BlockPos pFacingPos, CallbackInfoReturnable<BlockState> cir) {
        // TODO sync with FluidMixin
        // UPGRADE: copied from FlowingFluidBlock.updateShape with changes
        // change: do not check source, check canFlow and canHoldFluid
        if (FFluidStatic.canFlow(pLevel, pCurrentPos, pFacing) && FFluidStatic.canHoldFluid(pFacingState)) {
            FFluidStatic.scheduleFluidTick(pLevel, pCurrentPos);
        }
        cir.setReturnValue(super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos));
    }

    /*
        Roadblock
     */
    @Inject(method = "neighborChanged", at = @At(value = "HEAD"), cancellable = true)
    private void neighborChangedM(BlockState pState, World pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving, CallbackInfo ci) {
        // TODO sync with FluidMixin
        FFluidStatic.scheduleFluidTick(pLevel, pPos);
        ci.cancel();
    }
}