package net.skds.wpo.mixin.fluid;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.skds.wpo.fluidphysics.FFluidStatic;
import net.skds.wpo.fluidphysics.RenderStatic;
import net.skds.wpo.util.interfaces.IFlowingFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {FlowingFluid.class})
public class FlowingFluidMixin implements IFlowingFluid {

    /*
        Roadblock
     */
    @Inject(method = "tick", at = @At(value = "HEAD"), cancellable = true)
    public void tick(World worldIn, BlockPos pos, FluidState fs, CallbackInfo ci) {
        FFluidStatic.tickFlowingFluid(worldIn, pos, fs); // extract complexity
        ci.cancel(); // DONT run vanilla fluid tick (fluid spreading, duplication, etc.)
    }

    @Inject(method = "getFlow", at = @At(value = "HEAD"), cancellable = true)
    public void getFlow(IBlockReader w, BlockPos pos, FluidState fs, CallbackInfoReturnable<Vector3d> ci) {
        // .setReturnValue cancels the method injected into and overwrites the return value. No explicit return needed
        ci.setReturnValue(FFluidStatic.getVel(w, pos, fs));
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
}