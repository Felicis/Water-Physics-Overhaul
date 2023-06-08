package net.skds.wpo.mixin.entity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.spawner.WorldEntitySpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IronGolemEntity.class)
public abstract class IronGolemEntityMixin extends GolemEntity {
    protected IronGolemEntityMixin(EntityType<? extends GolemEntity> p_i48569_1_, World p_i48569_2_) {
        super(p_i48569_1_, p_i48569_2_);
    }

    /*
     * Roadblock Injection
     */
    @Inject(method = "checkSpawnObstruction", at = @At(value = "HEAD"), cancellable = true)
    private void checkSpawnObstructionM(IWorldReader pLevel, CallbackInfoReturnable<Boolean> cir) {
        // copied entire method because position comes from for loop: 1 change for fluid state
        BlockPos blockpos = this.blockPosition();
        BlockPos blockpos1 = blockpos.below();
        BlockState blockstate = pLevel.getBlockState(blockpos1);
        if (!blockstate.entityCanStandOn(pLevel, blockpos1, this)) {
            cir.setReturnValue(false);
        } else {
            for (int i = 1; i < 3; ++i) {
                BlockPos blockpos2 = blockpos.above(i);
                BlockState blockstate1 = pLevel.getBlockState(blockpos2);
                FluidState fluidState = pLevel.getFluidState(blockpos2); // added (& pass in next line)
                if (!WorldEntitySpawner.isValidEmptySpawnBlock(pLevel, blockpos2, blockstate1, fluidState, EntityType.IRON_GOLEM)) {
                    cir.setReturnValue(false);
                }
            }

            cir.setReturnValue(WorldEntitySpawner.isValidEmptySpawnBlock(pLevel, blockpos, pLevel.getBlockState(blockpos), Fluids.EMPTY.defaultFluidState(), EntityType.IRON_GOLEM) && pLevel.isUnobstructed(this));
        }
    }
}
