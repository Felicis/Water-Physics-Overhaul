package net.skds.wpo.mixin.block.withfluidphysics.notvanillawaterloggable;

import net.minecraft.block.BlockState;
import net.minecraft.block.FlowerPotBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.skds.wpo.fluidphysics.FFluidStatic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FlowerPotBlock.class)
public class FlowerPotBlockMixin {
    // not needed anymore, because setBlock is redirected and adapts to containing fluid OR displaces
//    @Redirect(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlock(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
//    private boolean useSetBlockProxy(World w, BlockPos pos, BlockState newState, int flags){
//        if (!w.isClientSide){
//            newState = FFluidStatic.copyFluidOrEject((ServerWorld) w, pos, newState);
////            FlowerPotBlock block = (FlowerPotBlock) newState.getBlock();
////            this.content == Blocks.AIR ? super.getCloneItemStack(pLevel, pPos, pState) : new ItemStack(this.content);
//            return w.setBlock(pos, newState, flags);
//        } else {// do nothing (return false) when client (see World.setBlock)
//            return false;
//        }
//    }
}
