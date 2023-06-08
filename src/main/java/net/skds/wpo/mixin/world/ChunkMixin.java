package net.skds.wpo.mixin.world;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.IChunk;
import net.minecraftforge.common.capabilities.CapabilityProvider;
import net.minecraftforge.common.extensions.IForgeChunk;
import net.skds.wpo.mixin.fluid.FluidStateMixinInterface;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

import static net.minecraft.world.chunk.Chunk.EMPTY_SECTION;

@Mixin(Chunk.class)
public abstract class ChunkMixin extends CapabilityProvider<Chunk> implements IChunkMixinInterface, IChunk, IForgeChunk {
    @Shadow
    @Final
    private ChunkSection[] sections;

    @Shadow
    @Final
    private World level;

    @Shadow
    private volatile boolean unsaved;

    @Shadow
    public abstract BlockState getBlockState(BlockPos pPos);

    @Shadow
    public abstract FluidState getFluidState(BlockPos pPos);

    protected ChunkMixin(Class<Chunk> baseClass) {
        super(baseClass);
    }

    @Nullable
    @Override
    public FluidState setFluidState(BlockPos pPos, FluidState newFluidState, boolean pIsMoving) {
        // UPGRADE: copied from Chunk.java#setBlockState(...)
        // some variable renames
        int i = pPos.getX() & 15;
        int j = pPos.getY();
        int k = pPos.getZ() & 15;
        ChunkSection chunksection = this.sections[j >> 4];
        if (chunksection == EMPTY_SECTION) {
            if (newFluidState.isEmpty()) {
                return null;
            }

            chunksection = new ChunkSection(j >> 4 << 4);
            this.sections[j >> 4] = chunksection;
        }

        boolean emptyBefore = chunksection.isEmpty();
        FluidState oldFluidState = ((ChunkSectionMixinInterface) chunksection).setFluidState(i, j & 15, k, newFluidState); // change & cast
        if (oldFluidState == newFluidState) {
            return null;
        } else {
            Fluid newFluid = newFluidState.getType();
            Fluid oldFluid = oldFluidState.getType();
            // removed 4 lines heightmap updates
            boolean emptyAfter = chunksection.isEmpty();
            if (emptyBefore != emptyAfter) { // emptiness changed => update chunk lighting
                this.level.getChunkSource().getLightEngine().updateSectionStatus(pPos, emptyAfter);
            }

            if (!this.level.isClientSide) {
                ((FluidStateMixinInterface) (Object) oldFluidState).onRemove(this.level, pPos, newFluidState, pIsMoving); // ugly cast
            }
            // change: removed 2 lines (tileentity)

            FluidStateMixinInterface fluidStateAfter = (FluidStateMixinInterface) (Object) chunksection.getFluidState(i, j & 15, k);
            if (!fluidStateAfter.is(newFluid)) { // change: split line to cast
                return null;
            } else {
                // removed 6 lines (tile entity)
                if (!this.level.isClientSide) {
                    ((FluidStateMixinInterface) (Object) newFluidState).onPlace(this.level, pPos, oldFluidState, pIsMoving); // ugly cast
                }
                // removed 9 lines tile entity

                this.unsaved = true;
                return oldFluidState;
            }
        }
    }

    /*
    skipping postProcessGeneration injection, because there is no need to update the fluidstate depending on the neighbors
    when transitioning from not ticking to ticking (chunk).
    e.g.
        BlockState blockstate = this.getBlockState(blockpos);
        BlockState blockstate1 = Block.updateFromNeighbourShapes(blockstate, this.level, blockpos);
        this.level.setBlock(blockpos, blockstate1, 20); // 20 means no neighbor reaction and no re-render
     */
}
