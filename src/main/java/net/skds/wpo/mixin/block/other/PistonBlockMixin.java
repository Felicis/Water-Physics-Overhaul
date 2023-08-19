package net.skds.wpo.mixin.block.other;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonBlock;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.skds.wpo.fluidphysics.FluidStatic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

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

    // TODO check if making MovingPistonBlock fluidloggable fixes problem more elegenatly?
    @Redirect(method = "moveBlocks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlock(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"),
            slice = @Slice(from = @At(value = "INVOKE", target = "Ljava/util/Map;keySet()Ljava/util/Set;")))
    private boolean setBlock_AndFluid(World instance, BlockPos pPos, BlockState pNewState, int pFlags) {
        // !!!!!!!! adapt to fluids here, because ever though is moving, does not change anymore (vacated by piston movement)
        // this means the moving block check in FFluidStatic.setBlockAlsoFluid() will be wrong
        assert pNewState.is(Blocks.AIR); // vacated by piston is set to air => adapt fluids
        FluidState fluidState = instance.getFluidState(pPos);
        return FluidStatic.setBlockAndFluid(instance, pPos, pNewState, fluidState, false, pFlags); // displacing not necessary, because setting to air
    }
}
