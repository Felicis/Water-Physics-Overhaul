package net.skds.wpo.mixin.block.other;

import net.minecraft.block.BlockState;
import net.minecraft.block.BushBlock;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BushBlock.class)
public class BushBlockMixin {
    @Redirect(method = "propagatesSkylightDown", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;getFluidState()Lnet/minecraft/fluid/FluidState;"))
    private FluidState getFluidState(BlockState instance, BlockState pState, IBlockReader pReader, BlockPos pPos) {
        return pReader.getFluidState(pPos);
    }
}
