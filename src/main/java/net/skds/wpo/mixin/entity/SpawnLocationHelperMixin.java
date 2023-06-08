package net.skds.wpo.mixin.entity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.SpawnLocationHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpawnLocationHelper.class)
public class SpawnLocationHelperMixin {
    /*
        Roadblock injection
     */
    @Inject(method = "getOverworldRespawnPos", at = @At(value = "HEAD"))
    private static void getOverworldRespawnPosM(ServerWorld p_241092_0_, int p_241092_1_, int p_241092_2_, boolean p_241092_3_, CallbackInfoReturnable<BlockPos> cir) {
        // copied because loop: 1 change
        BlockPos.Mutable blockpos$mutable = new BlockPos.Mutable(p_241092_1_, 0, p_241092_2_);
        Biome biome = p_241092_0_.getBiome(blockpos$mutable);
        boolean flag = p_241092_0_.dimensionType().hasCeiling();
        BlockState blockstate = biome.getGenerationSettings().getSurfaceBuilderConfig().getTopMaterial();
        if (p_241092_3_ && !blockstate.getBlock().is(BlockTags.VALID_SPAWN)) {
            cir.setReturnValue(null);
        } else {
            Chunk chunk = p_241092_0_.getChunk(p_241092_1_ >> 4, p_241092_2_ >> 4);
            int i = flag ? p_241092_0_.getChunkSource().getGenerator().getSpawnHeight() : chunk.getHeight(Heightmap.Type.MOTION_BLOCKING, p_241092_1_ & 15, p_241092_2_ & 15);
            if (i < 0) {
                cir.setReturnValue(null);
            } else {
                int j = chunk.getHeight(Heightmap.Type.WORLD_SURFACE, p_241092_1_ & 15, p_241092_2_ & 15);
                if (j <= i && j > chunk.getHeight(Heightmap.Type.OCEAN_FLOOR, p_241092_1_ & 15, p_241092_2_ & 15)) {
                    cir.setReturnValue(null);
                } else {
                    for (int k = i + 1; k >= 0; --k) {
                        blockpos$mutable.set(p_241092_1_, k, p_241092_2_);
                        BlockState blockstate1 = p_241092_0_.getBlockState(blockpos$mutable);
                        if (!p_241092_0_.getFluidState(blockpos$mutable).isEmpty()) { // XXXXXX CHANGED
                            break;
                        }

                        if (blockstate1.equals(blockstate)) {
                            cir.setReturnValue(blockpos$mutable.above().immutable());
                        }
                    }

                    cir.setReturnValue(null);
                }
            }
        }

    }
}
