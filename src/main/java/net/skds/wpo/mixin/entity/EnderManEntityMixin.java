package net.skds.wpo.mixin.entity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.monster.EndermanEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EndermanEntity.class)
public abstract class EnderManEntityMixin extends MonsterEntity {
    protected EnderManEntityMixin(EntityType<? extends MonsterEntity> p_i48553_1_, World p_i48553_2_) {
        super(p_i48553_1_, p_i48553_2_);
    }

    @Redirect(method = "teleport(DDD)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;getFluidState()Lnet/minecraft/fluid/FluidState;"))
    private FluidState getFluidStateM(BlockState instance, double pX, double pY, double pZ) {
        // move down blockpos (copied because no access to variable)
        BlockPos.Mutable pos = new BlockPos.Mutable(pX, pY, pZ);
        while (pos.getY() > 0 && !this.level.getBlockState(pos).getMaterial().blocksMotion()) {
            pos.move(Direction.DOWN);
        }
        return this.level.getFluidState(pos); // correct fluidstate access
    }
}
