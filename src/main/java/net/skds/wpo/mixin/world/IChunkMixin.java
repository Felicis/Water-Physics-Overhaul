package net.skds.wpo.mixin.world;

import net.minecraft.world.IBlockReader;
import net.minecraft.world.IStructureReader;
import net.minecraft.world.chunk.IChunk;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(IChunk.class)
public interface IChunkMixin extends IChunkMixinInterface, IBlockReader, IStructureReader {
    // IChunkMixinInterface adds methods
}
