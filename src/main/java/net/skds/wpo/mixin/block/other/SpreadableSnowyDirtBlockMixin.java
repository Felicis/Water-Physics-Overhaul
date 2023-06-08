package net.skds.wpo.mixin.block.other;

import net.minecraft.block.BlockState;
import net.minecraft.block.SpreadableSnowyDirtBlock;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SpreadableSnowyDirtBlock.class)
public class SpreadableSnowyDirtBlockMixin {
    @Redirect(method = "canBeGrass", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;getFluidState()Lnet/minecraft/fluid/FluidState;"))
    private static FluidState getFluidState(BlockState instance, BlockState pState, IWorldReader pLevelReader, BlockPos pPos) {
        return pLevelReader.getFluidState(pPos.above());
    }
}
