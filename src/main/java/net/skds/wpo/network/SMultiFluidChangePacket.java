package net.skds.wpo.network;

import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;
import net.skds.wpo.mixin.fluid.FluidMixinInterface;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class SMultiFluidChangePacket { // implements IPacket<IClientPlayNetHandler> {
    private SectionPos sectionPos;
    private short[] positions;
    private FluidState[] states;

    public SMultiFluidChangePacket() {
    }

    public SMultiFluidChangePacket(SectionPos pSectionPos, ShortSet pChangedFluids, ChunkSection pLevelChunkSection) {
        this.sectionPos = pSectionPos;
        this.initFields(pChangedFluids.size());
        int i = 0;

        for (short short1 : pChangedFluids) {
            this.positions[i] = short1;
            this.states[i] = pLevelChunkSection.getFluidState(SectionPos.sectionRelativeX(short1), SectionPos.sectionRelativeY(short1), SectionPos.sectionRelativeZ(short1));
            ++i;
        }

    }

    public SMultiFluidChangePacket(PacketBuffer pBuffer) {
        this.sectionPos = SectionPos.of(pBuffer.readLong());
        int i = pBuffer.readVarInt();
        this.initFields(i);

        for (int j = 0; j < this.positions.length; ++j) {
            long k = pBuffer.readVarLong();
            this.positions[j] = (short) ((int) (k & 4095L));
            this.states[j] = Fluid.FLUID_STATE_REGISTRY.byId((int) (k >>> 12));
        }
    }

    private void initFields(int p_244309_1_) {
        this.positions = new short[p_244309_1_];
        this.states = new FluidState[p_244309_1_];
    }

    public static SMultiFluidChangePacket decode(PacketBuffer pBuffer) {
        return new SMultiFluidChangePacket(pBuffer);
    }

    public void encode(PacketBuffer pBuffer) {
        pBuffer.writeLong(this.sectionPos.asLong());
        pBuffer.writeVarInt(this.positions.length);

        for (int i = 0; i < this.positions.length; ++i) {
            pBuffer.writeVarLong((long) (FluidMixinInterface.getId(this.states[i]) << 12 | this.positions[i])); // mixin interface getId
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                // Make sure it's only executed on the physical client
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        ClientWPONetHandler.handleChunkFluidsUpdate(this, ctx))
        );
        ctx.get().setPacketHandled(true);
    }

    public void runUpdates(BiConsumer<BlockPos, FluidState> pConsumer) {
        BlockPos.Mutable blockpos$mutable = new BlockPos.Mutable();

        for (int i = 0; i < this.positions.length; ++i) {
            short short1 = this.positions[i];
            blockpos$mutable.set(this.sectionPos.relativeToBlockX(short1), this.sectionPos.relativeToBlockY(short1), this.sectionPos.relativeToBlockZ(short1));
            pConsumer.accept(blockpos$mutable, this.states[i]);
        }

    }
}
