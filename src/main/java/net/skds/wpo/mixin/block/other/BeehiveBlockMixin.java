package net.skds.wpo.mixin.block.other;

import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeehiveBlock.class)
public class BeehiveBlockMixin {
    @Redirect(method = "trySpawnDripParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;getFluidState()Lnet/minecraft/fluid/FluidState;",
            ordinal = 0))
    public FluidState getFluidState1(BlockState instance, World pLevel, BlockPos pPos) {
        return pLevel.getFluidState(pPos);
    }
    @Redirect(method = "trySpawnDripParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;getFluidState()Lnet/minecraft/fluid/FluidState;",
            ordinal = 1))
    public FluidState getFluidState2(BlockState instance, World pLevel, BlockPos pPos) {
        return pLevel.getFluidState(pPos.below());
    }
}
