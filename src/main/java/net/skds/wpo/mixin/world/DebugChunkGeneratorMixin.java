package net.skds.wpo.mixin.world;

import net.minecraft.fluid.FluidState;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.DebugChunkGenerator;
import net.minecraft.world.gen.settings.DimensionStructuresSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DebugChunkGenerator.class)
public abstract class DebugChunkGeneratorMixin extends ChunkGenerator {
    public DebugChunkGeneratorMixin(BiomeProvider pBiomeSource, DimensionStructuresSettings pSettings) {
        super(pBiomeSource, pSettings);
    }

    @Inject(method = "getBaseColumn", at = @At(value = "RETURN"))
    private void addFluidStates(int p_230348_1_, int p_230348_2_, CallbackInfoReturnable<IBlockReader> cir){
        BlockReaderMixinInterface blockReader = (BlockReaderMixinInterface) cir.getReturnValue(); // interface to use method here
        blockReader.addFluidStates(new FluidState[0]); // add fluid states so they can be accessed later
    }
}
