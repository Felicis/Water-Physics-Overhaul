package net.skds.wpo.mixin.item;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.AbstractMapItem;
import net.minecraft.item.FilledMapItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.storage.MapData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FilledMapItem.class)
public abstract class FilledMapItemMixin extends AbstractMapItem {
    @Shadow
    protected abstract BlockState getCorrectStateForFluidBlock(World pLevel, BlockState pState, BlockPos pPos);

    public FilledMapItemMixin(Properties p_i48514_1_) {
        super(p_i48514_1_);
    }

    @Redirect(method = "getCorrectStateForFluidBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;getFluidState()Lnet/minecraft/fluid/FluidState;"))
    private FluidState getFluidState(BlockState instance, World pLevel, BlockState pState, BlockPos pPos){
        return pLevel.getFluidState(pPos); // correct fluid state access
    }

    /*
      Roadblock Injection
     */
    @Inject(method = "update", at = @At(value = "HEAD"), cancellable = true)
    private void updateM(World pLevel, Entity pViewer, MapData pData, CallbackInfo ci) {
        // override because for loop: 2 changes
        if (pLevel.dimension() == pData.dimension && pViewer instanceof PlayerEntity) {
            int i = 1 << pData.scale;
            int j = pData.x;
            int k = pData.z;
            int l = MathHelper.floor(pViewer.getX() - (double) j) / i + 64;
            int i1 = MathHelper.floor(pViewer.getZ() - (double) k) / i + 64;
            int j1 = 128 / i;
            if (pLevel.dimensionType().hasCeiling()) {
                j1 /= 2;
            }

            MapData.MapInfo mapdata$mapinfo = pData.getHoldingPlayer((PlayerEntity) pViewer);
            ++mapdata$mapinfo.step;
            boolean flag = false;

            for (int k1 = l - j1 + 1; k1 < l + j1; ++k1) {
                if ((k1 & 15) == (mapdata$mapinfo.step & 15) || flag) {
                    flag = false;
                    double d0 = 0.0D;

                    for (int l1 = i1 - j1 - 1; l1 < i1 + j1; ++l1) {
                        if (k1 >= 0 && l1 >= -1 && k1 < 128 && l1 < 128) {
                            int i2 = k1 - l;
                            int j2 = l1 - i1;
                            boolean flag1 = i2 * i2 + j2 * j2 > (j1 - 2) * (j1 - 2);
                            int k2 = (j / i + k1 - 64) * i;
                            int l2 = (k / i + l1 - 64) * i;
                            Multiset<MaterialColor> multiset = LinkedHashMultiset.create();
                            Chunk chunk = pLevel.getChunkAt(new BlockPos(k2, 0, l2));
                            if (!chunk.isEmpty()) {
                                ChunkPos chunkpos = chunk.getPos();
                                int i3 = k2 & 15;
                                int j3 = l2 & 15;
                                int k3 = 0;
                                double d1 = 0.0D;
                                if (pLevel.dimensionType().hasCeiling()) {
                                    int l3 = k2 + l2 * 231871;
                                    l3 = l3 * l3 * 31287121 + l3 * 11;
                                    if ((l3 >> 20 & 1) == 0) {
                                        multiset.add(Blocks.DIRT.defaultBlockState().getMapColor(pLevel, BlockPos.ZERO), 10);
                                    } else {
                                        multiset.add(Blocks.STONE.defaultBlockState().getMapColor(pLevel, BlockPos.ZERO), 100);
                                    }

                                    d1 = 100.0D;
                                } else {
                                    BlockPos.Mutable blockpos$mutable1 = new BlockPos.Mutable();
                                    BlockPos.Mutable blockpos$mutable = new BlockPos.Mutable();

                                    for (int i4 = 0; i4 < i; ++i4) {
                                        for (int j4 = 0; j4 < i; ++j4) {
                                            int k4 = chunk.getHeight(Heightmap.Type.WORLD_SURFACE, i4 + i3, j4 + j3) + 1;
                                            BlockState blockstate;
                                            if (k4 <= 1) {
                                                blockstate = Blocks.BEDROCK.defaultBlockState();
                                            } else {
                                                do {
                                                    --k4;
                                                    blockpos$mutable1.set(chunkpos.getMinBlockX() + i4 + i3, k4, chunkpos.getMinBlockZ() + j4 + j3);
                                                    blockstate = chunk.getBlockState(blockpos$mutable1);
                                                } while (blockstate.getMapColor(pLevel, blockpos$mutable1) == MaterialColor.NONE && k4 > 0);

                                                if (k4 > 0 && !chunk.getFluidState(blockpos$mutable1).isEmpty()) { // XXXXXX CHANGE
                                                    int l4 = k4 - 1;
                                                    blockpos$mutable.set(blockpos$mutable1);

                                                    BlockState blockstate1;
                                                    do {
                                                        blockpos$mutable.setY(l4--);
                                                        blockstate1 = chunk.getBlockState(blockpos$mutable);
                                                        ++k3;
                                                    } while (l4 > 0 && !chunk.getFluidState(blockpos$mutable).isEmpty()); // XXXXXX CHANGE

                                                    blockstate = this.getCorrectStateForFluidBlock(pLevel, blockstate, blockpos$mutable1);
                                                }
                                            }

                                            pData.checkBanners(pLevel, chunkpos.getMinBlockX() + i4 + i3, chunkpos.getMinBlockZ() + j4 + j3);
                                            d1 += (double) k4 / (double) (i * i);
                                            multiset.add(blockstate.getMapColor(pLevel, blockpos$mutable1));
                                        }
                                    }
                                }

                                k3 = k3 / (i * i);
                                double d2 = (d1 - d0) * 4.0D / (double) (i + 4) + ((double) (k1 + l1 & 1) - 0.5D) * 0.4D;
                                int i5 = 1;
                                if (d2 > 0.6D) {
                                    i5 = 2;
                                }

                                if (d2 < -0.6D) {
                                    i5 = 0;
                                }

                                MaterialColor materialcolor = Iterables.getFirst(Multisets.copyHighestCountFirst(multiset), MaterialColor.NONE);
                                if (materialcolor == MaterialColor.WATER) {
                                    d2 = (double) k3 * 0.1D + (double) (k1 + l1 & 1) * 0.2D;
                                    i5 = 1;
                                    if (d2 < 0.5D) {
                                        i5 = 2;
                                    }

                                    if (d2 > 0.9D) {
                                        i5 = 0;
                                    }
                                }

                                d0 = d1;
                                if (l1 >= 0 && i2 * i2 + j2 * j2 < j1 * j1 && (!flag1 || (k1 + l1 & 1) != 0)) {
                                    byte b0 = pData.colors[k1 + l1 * 128];
                                    byte b1 = (byte) (materialcolor.id * 4 + i5);
                                    if (b0 != b1) {
                                        pData.colors[k1 + l1 * 128] = b1;
                                        pData.setDirty(k1, l1);
                                        flag = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
        ci.cancel(); // XXX return and do not execute method
    }
}
