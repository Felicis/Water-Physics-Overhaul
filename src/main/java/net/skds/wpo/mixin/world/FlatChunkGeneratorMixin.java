package net.skds.wpo.mixin.world;

import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.FlatChunkGenerator;
import net.minecraft.world.gen.FlatGenerationSettings;
import net.minecraft.world.gen.settings.DimensionStructuresSettings;
import net.skds.wpo.mixininterfaces.BlockReaderMixinInterface;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;

@Mixin(FlatChunkGenerator.class)
public abstract class FlatChunkGeneratorMixin extends ChunkGenerator {
    @Shadow @Final private FlatGenerationSettings settings;

    public FlatChunkGeneratorMixin(BiomeProvider pBiomeSource, DimensionStructuresSettings pSettings) {
        super(pBiomeSource, pSettings);
    }

    @Inject(method = "getBaseColumn", at = @At(value = "RETURN"))
    private void addFluidStates(int p_230348_1_, int p_230348_2_, CallbackInfoReturnable<IBlockReader> cir){
        BlockReaderMixinInterface blockReader = (BlockReaderMixinInterface) cir.getReturnValue(); // interface to use method here
        int length = this.settings.getLayers().length; // length of blockstate array from settings
        FluidState[] fluidStates = new FluidState[length];
        Arrays.fill(fluidStates, Fluids.EMPTY.defaultFluidState()); // create empty, because flat world has no water on generation ^~^
        blockReader.addFluidStates(fluidStates); // add fluid states so they can be accessed later
    }
}
