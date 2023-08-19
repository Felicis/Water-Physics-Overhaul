package net.skds.wpo.mixin.fluid;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.skds.wpo.fluidphysics.FluidStatic;
import net.skds.wpo.fluidphysics.RenderStatic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlowingFluid.class)
public abstract class FlowingFluidMixin extends Fluid {

    /*
        Roadblock
     */
    @Inject(method = "tick", at = @At(value = "HEAD"), cancellable = true)
    public void tick(World worldIn, BlockPos pos, FluidState fs, CallbackInfo ci) {
        FluidStatic.tickFlowingFluid(worldIn, pos, fs); // extract complexity
        ci.cancel(); // DONT run vanilla fluid tick (fluid spreading, duplication, etc.)
    }

    @Inject(method = "getFlow", at = @At(value = "HEAD"), cancellable = true)
    public void getFlow(IBlockReader w, BlockPos pos, FluidState fs, CallbackInfoReturnable<Vector3d> ci) {
        // .setReturnValue cancels the method injected into and overwrites the return value. No explicit return needed
        ci.setReturnValue(FluidStatic.getVel(w, pos, fs));
    }

    @Inject(method = "getOwnHeight", at = @At(value = "HEAD"), cancellable = true)
    public void getOwnHeight(FluidState fs, CallbackInfoReturnable<Float> ci) {
        // .setReturnValue cancels the method injected into and overwrites the return value. No explicit return needed
        ci.setReturnValue(RenderStatic.getHeight(fs.getAmount()));
    }

    public void beforeReplacingBlockCustom(IWorld worldIn, BlockPos pos, BlockState state) {
        beforeDestroyingBlock(worldIn, pos, state);

    }

    // ================= SHADOW ================ //

    @Shadow
    protected void beforeDestroyingBlock(IWorld worldIn, BlockPos pos, BlockState state) {
    }

//    private void spread(IWorld pLevel, BlockPos pPos, FluidState pState) {}
//    private void canSpreadTo(IWorld pLevel, BlockPos pFromPos, BlockState pFromState,
//                             Direction direction, BlockPos pToPos, BlockState pToState, FluidState pToFState, Fluid pFromFluid) {}
//    // canHoldFluid: contains check for material.blocksMotion(), which breaks e.g. redstone (DECORATION), vines (REPLACEABLE_PLANT), ...
//    private void canHoldFluid(IBlockReader pLevel, BlockPos pPos, BlockState pState, Fluid pNewFluid) {}
}