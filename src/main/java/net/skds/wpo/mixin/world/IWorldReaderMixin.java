package net.skds.wpo.mixin.world;

import net.minecraft.fluid.FluidState;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraft.world.ICollisionReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.biome.BiomeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IWorldReader.class)
public interface IWorldReaderMixin extends IWorldReaderMixinInterface, IBlockDisplayReader, ICollisionReader, BiomeManager.IBiomeReader {
    // containsAnyLiquid: cannot inject into interfaces => override in World, ServerWorld, ClientWorld, WorldGenRegion & call containsAnyLiquidM

    // TODO: mixin?
//    default boolean isWaterAt(BlockPos pPos) {
//        return this.getFluidState(pPos).is(FluidTags.WATER);
//    }
}
