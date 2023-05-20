package net.skds.wpo.mixins.compat115plus;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowerPotBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.skds.wpo.fluidphysics.FFluidStatic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FlowerPotBlock.class)
public class FlowerPotBlockMixin {
    @Redirect(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlock(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    private boolean useSetBlockProxy(World w, BlockPos pos, BlockState oldState, int flags){
        BlockState newState = FFluidStatic.getStateWithFluid(oldState, w, pos);
//        FlowerPotBlock block = (FlowerPotBlock) oldState.getBlock();
//        this.content == Blocks.AIR ? super.getCloneItemStack(pLevel, pPos, pState) : new ItemStack(this.content);
        return w.setBlock(pos, newState, flags);
    }
}
