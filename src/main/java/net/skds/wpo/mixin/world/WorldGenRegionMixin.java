package net.skds.wpo.mixin.world;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.gen.WorldGenRegion;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldGenRegion.class)
public abstract class WorldGenRegionMixin implements IWorldWriterMixinInterface, ISeedReader {

    @Shadow
    @Final
    private ServerWorld level;

    @Shadow
    public abstract FluidState getFluidState(BlockPos pPos);

    @Shadow
    public abstract boolean setBlock(BlockPos pPos, BlockState pState, int pFlags, int pRecursionLeft);

    @Override
    public boolean setFluid(BlockPos pPos, FluidState pState, int pFlags, int pRecursionLeft) {
        IChunkMixinInterface ichunk = (IChunkMixinInterface) this.getChunk(pPos);
        FluidState fluidState = ichunk.setFluidState(pPos, pState, false);
        // skip this.level.onFluidStateChange, because only update POIs (see WorldMixin.java)
        return true;
    }

    @Override
    public boolean removeFluid(BlockPos pPos, boolean pIsMoving) {
        return this.setFluid(pPos, Fluids.EMPTY.defaultFluidState(), 3);
    }

    @Redirect(method = "removeBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/gen/WorldGenRegion;setBlock(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    private boolean setBlockConsideringFluidState(WorldGenRegion instance, BlockPos pPos, BlockState airBlockState, int i3) {
        // replace with contained fluid block (like in World.java)
        // feels safer, since we want to add automatic fluidstates to underwater plants in worldgen and fluid should be removed separately
        return this.setBlock(pPos, this.getFluidState(pPos).createLegacyBlock(), 3);
    }

    @Redirect(method = "destroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/gen/WorldGenRegion;setBlock(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;II)Z"))
    private boolean setBlockConsideringFluidState2(WorldGenRegion instance, BlockPos pPos, BlockState airBlockState, int i3, int pRecursionLeft) {
        // replace with contained fluid block (like in World.java)
        // feels safer, since we want to add automatic fluidstates to underwater plants in worldgen and fluid should be removed separately
        return this.setBlock(pPos, this.getFluidState(pPos).createLegacyBlock(), 3, pRecursionLeft);
    }

    // mixin? only needed when changing worldgen
//    public boolean isStateAtPosition(BlockPos pPos, Predicate<BlockState> pState) {
//        return pState.test(this.getBlockState(pPos));
//    }
}
