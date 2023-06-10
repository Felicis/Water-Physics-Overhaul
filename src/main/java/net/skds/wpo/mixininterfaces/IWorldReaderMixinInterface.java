package net.skds.wpo.mixininterfaces;

import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IWorldReader;

public interface IWorldReaderMixinInterface {
    default boolean containsAnyLiquidM(AxisAlignedBB pBb) {
        // because for loop copied with 1 change
        int i = MathHelper.floor(pBb.minX);
        int j = MathHelper.ceil(pBb.maxX);
        int k = MathHelper.floor(pBb.minY);
        int l = MathHelper.ceil(pBb.maxY);
        int i1 = MathHelper.floor(pBb.minZ);
        int j1 = MathHelper.ceil(pBb.maxZ);
        BlockPos.Mutable blockpos$mutable = new BlockPos.Mutable();

        for (int k1 = i; k1 < j; ++k1) {
            for (int l1 = k; l1 < l; ++l1) {
                for (int i2 = i1; i2 < j1; ++i2) {
                    FluidState fluidState = ((IWorldReader) this).getFluidState(blockpos$mutable.set(k1, l1, i2)); // CHANGE 1a
                    if (!fluidState.isEmpty()) { // CHANGE 1b
                        return true;
                    }
                }
            }
        }

        return false;
    }

}
