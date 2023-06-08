package net.skds.wpo.mixin.world;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.NoiseChunkGenerator;
import net.minecraft.world.gen.settings.DimensionStructuresSettings;
import net.skds.wpo.fluidphysics.FFluidStatic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Arrays;

@Mixin(NoiseChunkGenerator.class)
public abstract class NoiseChunkGeneratorMixin extends ChunkGenerator {
    public NoiseChunkGeneratorMixin(BiomeProvider pBiomeSource, DimensionStructuresSettings pSettings) {
        super(pBiomeSource, pSettings);
    }

    // add fluid states to block reader
    @Inject(method = "getBaseColumn", at = @At(value = "RETURN"), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void getBaseColumnM(int p_230348_1_, int p_230348_2_, CallbackInfoReturnable<IBlockReader> cir, BlockState[] ablockstate) {
        BlockReaderMixinInterface blockReader = (BlockReaderMixinInterface) cir.getReturnValue(); // interface to use method here
        FluidState[] fluidStates = Arrays.stream(ablockstate).sequential()
                .map(FFluidStatic::getDefaultFluidState)
                .toArray(FluidState[]::new);
        blockReader.addFluidStates(fluidStates); // add fluid states so they can be accessed later
    }

    // add fluid states to chunk section
    @Redirect(method = "fillFromNoise", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/ChunkSection;setBlockState(IIILnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;"))
    private BlockState setBlockStateAndFluidState(ChunkSection chunkSection, int pX, int pY, int pZ, BlockState blockstate, boolean useLocks) {
        chunkSection.setBlockState(pX, pY, pZ, blockstate, useLocks);
        ((ChunkSectionMixinInterface) chunkSection).setFluidState(pX, pY, pZ, FFluidStatic.getDefaultFluidState(blockstate), useLocks);
        return null; // unused by method
    }

    // public buildSurfaceAndBedrock: HANDLED BY Biome.buildSurfaceAt (should add fluidstates to chunk)
}
