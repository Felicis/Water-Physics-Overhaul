package net.skds.wpo.mixin.world;

import it.unimi.dsi.fastutil.shorts.ShortArraySet;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.server.ChunkHolder;
import net.skds.wpo.mixininterfaces.ChunkHolderMixinInterface;
import net.skds.wpo.network.PacketHandler;
import net.skds.wpo.network.SChangeFluidPacket;
import net.skds.wpo.network.SMultiFluidChangePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(ChunkHolder.class)
public abstract class ChunkHolderMixin implements ChunkHolderMixinInterface {
    @Shadow
    @Nullable
    public abstract Chunk getTickingChunk();

    private boolean hasFluidChangedSections;
    private final ShortSet[] changedFluidsPerSection = new ShortSet[16];

    @Override
    public void fluidChanged(BlockPos pPos) {
        // created new Shortset and boolean for fluids (fluids have separate update packets from blocks)
        Chunk chunk = this.getTickingChunk();
        if (chunk != null) {
            byte b0 = (byte) SectionPos.blockToSectionCoord(pPos.getY());
            if (this.changedFluidsPerSection[b0] == null) {
                this.hasFluidChangedSections = true;
                this.changedFluidsPerSection[b0] = new ShortArraySet();
            }
            this.changedFluidsPerSection[b0].add(SectionPos.sectionRelativePos(pPos));
        }
    }

    @Inject(method = "broadcastChanges", at = @At(value = "HEAD"))
    private void alsoBroadcastFluidChanges(Chunk pChunk, CallbackInfo ci) {
        // copied from ChunkHolder.broadcastChanges(Chunk) with changes
        // TODO check whether light skipping is okay, else add again (also to fluid packets)
        if (this.hasFluidChangedSections) { // skip light checks (already done in block version of method)
            World world = pChunk.getLevel();

            // skip light computation
            // skip sending light packet

            for (int k = 0; k < this.changedFluidsPerSection.length; ++k) {
                ShortSet shortset = this.changedFluidsPerSection[k];
                if (shortset != null) {
                    SectionPos sectionpos = SectionPos.of(pChunk.getPos(), k);
                    if (shortset.size() == 1) {
                        BlockPos blockpos = sectionpos.relativeToBlockPos(shortset.iterator().nextShort());
                        FluidState fluidState = world.getFluidState(blockpos);
                        PacketHandler.sendTrackingChunk(pChunk, new SChangeFluidPacket(blockpos, fluidState)); // new channel and handler + new packet
                        // skip blockentity
                    } else {
                        ChunkSection chunksection = pChunk.getSections()[sectionpos.getY()];
                        SMultiFluidChangePacket multifluidchangepacket = new SMultiFluidChangePacket(sectionpos, shortset, chunksection); // new packet
                        PacketHandler.sendTrackingChunk(pChunk, multifluidchangepacket); // new channel and handler
                        // skip blockentity
                    }
                    this.changedFluidsPerSection[k] = null;
                }
            }
            this.hasFluidChangedSections = false;
        }
    }
}
