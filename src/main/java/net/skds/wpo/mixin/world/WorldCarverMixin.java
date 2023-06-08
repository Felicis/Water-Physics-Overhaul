package net.skds.wpo.mixin.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FluidState;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.carver.WorldCarver;
import net.minecraftforge.registries.ForgeRegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.BitSet;
import java.util.Random;
import java.util.function.Function;

@Mixin(WorldCarver.class)
public abstract class WorldCarverMixin extends ForgeRegistryEntry<WorldCarver<?>> {
    @Shadow
    protected abstract boolean canReplaceBlock(BlockState pState);

    // UPGRADE: this is redirecting the only usage of canReplaceBlock(BlockState, BlockState). if new ones are added, have to be corrected, too
    @Redirect(method = "carveBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/gen/carver/WorldCarver;canReplaceBlock(Lnet/minecraft/block/BlockState;Lnet/minecraft/block/BlockState;)Z"))
    private boolean canReplaceBlockM(WorldCarver instance, BlockState pState, BlockState pAboveState, IChunk chunk,
                                     Function<BlockPos, Biome> p_230358_2_, BitSet p_230358_3_, Random p_230358_4_,
                                     BlockPos.Mutable pos, BlockPos.Mutable posAbove) {
        // pos and posAbove are modified in the method previous to the injection, but mutable, thus the references are correct
        BlockState bState = chunk.getBlockState(pos);
        FluidState fStateAbove = chunk.getFluidState(posAbove);
        return this.canReplaceBlock(bState) || (bState.is(Blocks.SAND) || bState.is(Blocks.GRAVEL)) && !fStateAbove.is(FluidTags.WATER);
    }
}
