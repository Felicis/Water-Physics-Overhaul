package net.skds.wpo.mixininterfaces;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;

public interface FlowingFluidMixinInterface {
    void beforeDestroyingBlockCustom(IWorld worldIn, BlockPos pos, BlockState state);
}
