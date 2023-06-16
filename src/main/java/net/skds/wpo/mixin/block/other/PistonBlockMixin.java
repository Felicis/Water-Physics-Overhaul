package net.skds.wpo.mixin.block.other;

import net.minecraft.block.PistonBlock;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PistonBlock.class)
public class PistonBlockMixin {
    // not needed because PistonDisplacer already whitelists all pos's free after piston movement (vacated by sticky/slime... or piston head)
    // and blacklists all pos's blocked after piston movement (destroyed, pushed to, piston head)
//    @Redirect(method = "moveBlocks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlock(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z",
//            ordinal = 0), slice = @Slice(from = @At(value = "INVOKE", target = "Ljava/util/Map;keySet()Ljava/util/Set;")))
//    private boolean setBlockWithFluid(World world, BlockPos pPos, BlockState pNewBlockState, int pFlags) {
//        // ORIGINAL: pLevel.setBlock(blockpos4, blockstate3, 82);
//        // the setBlock that sets the pos left in the map (pushed from and left empty) to AIR
//        FluidState fluidState = world.getFluidState(pPos);
//        return FFluidStatic.setFluidForBlock(world, pPos, fluidState, pFlags);
//    }
}
