package net.skds.wpo.mixin.world;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.palette.PalettedContainer;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.storage.ChunkSerializer;
import net.skds.wpo.mixininterfaces.ChunkSectionMixinInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChunkSerializer.class)
public abstract class ChunkSerializerMixin {
    private static CompoundNBT sectionCompoundNbt; // THIS IS ONLY SAFE IF NOT THREADED!!!!
    private static final String fluidPaletteKey = "PaletteFluids";
    private static final String fluidStatesKey = "FluidStates";

    @Redirect(method = "read", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundNBT;getByte(Ljava/lang/String;)B"))
    private static byte read_getByte_cache_variable(CompoundNBT compoundnbt1, String pKey) {
        sectionCompoundNbt = compoundnbt1; // cache value for later mixin (read)
        return compoundnbt1.getByte("Y"); // original code
    }

    @Redirect(method = "read", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/ChunkSection;getStates()Lnet/minecraft/util/palette/PalettedContainer;"))
    private static PalettedContainer<BlockState> read_getStates_AlsoFluids(ChunkSection chunkSection) {
        // read fluids from nbt into chunk section (copied from block version)
        if (sectionCompoundNbt.contains(fluidPaletteKey, 9) && sectionCompoundNbt.contains(fluidStatesKey, 12)) {
            ((ChunkSectionMixinInterface) chunkSection).getFluidStates()
                    .read(sectionCompoundNbt.getList(fluidPaletteKey, 10), sectionCompoundNbt.getLongArray(fluidStatesKey));
        }
        // original code
        return chunkSection.getStates();
    }

    @Redirect(method = "write", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundNBT;putByte(Ljava/lang/String;B)V"))
    private static void write_putByte_cache_variable(CompoundNBT compoundnbt2, String pKey, byte pValue) {
        sectionCompoundNbt = compoundnbt2; // cache value for later mixin (write)
        compoundnbt2.putByte(pKey, pValue); // original code
    }

    @Redirect(method = "write", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/ChunkSection;getStates()Lnet/minecraft/util/palette/PalettedContainer;"))
    private static PalettedContainer<BlockState> write_getStates_AlsoFluids(ChunkSection chunkSection) {
        // write fluids from chunk section into nbt (copied from block version)
        ((ChunkSectionMixinInterface) chunkSection).getFluidStates().write(sectionCompoundNbt, fluidPaletteKey, fluidStatesKey);
        // original code
        return chunkSection.getStates();
    }
}
