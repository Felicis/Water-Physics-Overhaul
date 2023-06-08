package net.skds.wpo.mixin.entity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FishingBobberEntity.class)
public abstract class FishingBobberEntityMixin extends Entity { // this.level requires Entity, not ProjectileEntity (which has a private constructor...)

    public FishingBobberEntityMixin(EntityType<?> pType, World pLevel) {
        super(pType, pLevel);
    }

    @Redirect(method = "getOpenWaterTypeForBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;getFluidState()Lnet/minecraft/fluid/FluidState;"))
    private FluidState getFluidState(BlockState instance, BlockPos pos) {
        return this.level.getFluidState(pos);
    }
}
