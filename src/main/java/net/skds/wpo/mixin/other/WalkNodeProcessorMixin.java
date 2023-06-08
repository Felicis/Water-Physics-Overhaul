package net.skds.wpo.mixin.other;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.MobEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.pathfinding.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(WalkNodeProcessor.class)
public abstract class WalkNodeProcessorMixin extends NodeProcessor {

    @Shadow protected abstract PathNodeType getCachedBlockType(MobEntity pEntity, int pX, int pY, int pZ);

    @Shadow protected abstract boolean hasPositiveMalus(BlockPos p_237239_1_);

    @Shadow protected abstract PathNodeType getBlockPathType(MobEntity pEntityliving, BlockPos pPos);

    public PathPoint getStart() {
        // copied with 3 changes
        BlockPos.Mutable blockpos$mutable = new BlockPos.Mutable();
        int i = MathHelper.floor(this.mob.getY());
        BlockState blockstate = this.level.getBlockState(blockpos$mutable.set(this.mob.getX(), (double)i, this.mob.getZ()));
        FluidState fluidState = this.level.getFluidState(blockpos$mutable.set(this.mob.getX(), (double)i, this.mob.getZ())); // CHANGE 1a
        if (!this.mob.canStandOnFluid(fluidState.getType())) { // CHANGE 1b
            if (this.canFloat() && this.mob.isInWater()) {
                while(true) {
                    if (blockstate.getBlock() != Blocks.WATER && fluidState != Fluids.WATER.getSource(false)) { // CHANGE 2a
                        --i;
                        break;
                    }

                    ++i;
                    blockstate = this.level.getBlockState(blockpos$mutable.set(this.mob.getX(), (double)i, this.mob.getZ()));
                    fluidState = this.level.getFluidState(blockpos$mutable.set(this.mob.getX(), (double)i, this.mob.getZ())); // CHANGE 2b
                }
            } else if (this.mob.isOnGround()) {
                i = MathHelper.floor(this.mob.getY() + 0.5D);
            } else {
                BlockPos blockpos;
                for(blockpos = this.mob.blockPosition(); (this.level.getBlockState(blockpos).isAir() || this.level.getBlockState(blockpos).isPathfindable(this.level, blockpos, PathType.LAND)) && blockpos.getY() > 0; blockpos = blockpos.below()) {
                }

                i = blockpos.above().getY();
            }
        } else {
            while(this.mob.canStandOnFluid(fluidState.getType())) { // CHANGE 3a
                ++i;
                blockstate = this.level.getBlockState(blockpos$mutable.set(this.mob.getX(), (double)i, this.mob.getZ()));
                fluidState = this.level.getFluidState(blockpos$mutable.set(this.mob.getX(), (double)i, this.mob.getZ())); // CHANGE 3b
            }

            --i;
        }

        BlockPos blockpos1 = this.mob.blockPosition();
        PathNodeType pathnodetype = this.getCachedBlockType(this.mob, blockpos1.getX(), i, blockpos1.getZ());
        if (this.mob.getPathfindingMalus(pathnodetype) < 0.0F) {
            AxisAlignedBB axisalignedbb = this.mob.getBoundingBox();
            if (this.hasPositiveMalus(blockpos$mutable.set(axisalignedbb.minX, (double)i, axisalignedbb.minZ)) || this.hasPositiveMalus(blockpos$mutable.set(axisalignedbb.minX, (double)i, axisalignedbb.maxZ)) || this.hasPositiveMalus(blockpos$mutable.set(axisalignedbb.maxX, (double)i, axisalignedbb.minZ)) || this.hasPositiveMalus(blockpos$mutable.set(axisalignedbb.maxX, (double)i, axisalignedbb.maxZ))) {
                PathPoint pathpoint = this.getNode(blockpos$mutable);
                pathpoint.type = this.getBlockPathType(this.mob, pathpoint.asBlockPos());
                pathpoint.costMalus = this.mob.getPathfindingMalus(pathpoint.type);
                return pathpoint;
            }
        }

        PathPoint pathpoint1 = this.getNode(blockpos1.getX(), i, blockpos1.getZ());
        pathpoint1.type = this.getBlockPathType(this.mob, pathpoint1.asBlockPos());
        pathpoint1.costMalus = this.mob.getPathfindingMalus(pathpoint1.type);
        return pathpoint1;
    }

}
