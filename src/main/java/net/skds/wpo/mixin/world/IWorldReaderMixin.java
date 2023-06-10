package net.skds.wpo.mixin.world;

import net.minecraft.world.IBlockDisplayReader;
import net.minecraft.world.ICollisionReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.biome.BiomeManager;
import net.skds.wpo.mixininterfaces.IWorldReaderMixinInterface;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(IWorldReader.class)
public interface IWorldReaderMixin extends IWorldReaderMixinInterface, IBlockDisplayReader, ICollisionReader, BiomeManager.IBiomeReader {
    // containsAnyLiquid: cannot inject into interfaces => override in World, ServerWorld, ClientWorld, WorldGenRegion & call containsAnyLiquidM

    // TODO: mixin?
//    default boolean isWaterAt(BlockPos pPos) {
//        return this.getFluidState(pPos).is(FluidTags.WATER);
//    }
}
