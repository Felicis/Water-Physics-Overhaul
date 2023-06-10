package net.skds.wpo.mixin.block.other;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.skds.wpo.mixininterfaces.FluidStateMixinInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Block.class)
public class BlockMixin {
    @Redirect(method = "propagatesSkylightDown", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;getFluidState()Lnet/minecraft/fluid/FluidState;"))
    public FluidState getFluidState(BlockState instance, BlockState pState, IBlockReader pReader, BlockPos pPos) {
        return pReader.getFluidState(pPos);
    }

    @Redirect(method = "shouldRenderFace", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;skipRendering(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/Direction;)Z"))
    private static boolean skipRenderingM(BlockState origBlockState, BlockState origBlockState1, Direction origDirection,
                                          BlockState pBlockState, IBlockReader pReader, BlockPos pPos, Direction pDir) {
        boolean skipRenderingBlock = origBlockState.skipRendering(origBlockState1, origDirection);
        // also check
        FluidState fluidState = pReader.getFluidState(pPos);
        FluidState adjacentFluidState = pReader.getFluidState(pPos.relative(pDir));
        boolean skipRenderingFluid = ((FluidStateMixinInterface)(Object) fluidState).skipRendering(adjacentFluidState, origDirection);
        return skipRenderingBlock && skipRenderingFluid;
    }
}