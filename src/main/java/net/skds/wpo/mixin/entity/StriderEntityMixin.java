package net.skds.wpo.mixin.entity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.passive.StriderEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(StriderEntity.class)
public class StriderEntityMixin {
    @Redirect(method = "getWalkTargetValue", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;getFluidState()Lnet/minecraft/fluid/FluidState;"))
    private FluidState getFluidState(BlockState instance, BlockPos pPos, IWorldReader pLevel){
        return pLevel.getFluidState(pPos);
    }
}
