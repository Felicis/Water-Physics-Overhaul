package net.skds.wpo.network;

import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;
import net.skds.wpo.mixininterfaces.FluidMixinInterface;

import java.util.function.Supplier;

public class SChangeFluidPacket { // implements IPacket<IClientPlayNetHandler> {
    // UPGRADE: copied from SChangeBlockPacket with some changes
    private BlockPos pos;
    private FluidState fluidState;

    public SChangeFluidPacket() {
    }

    public SChangeFluidPacket(BlockPos pPos, FluidState pFluidState) {
        this.pos = pPos;
        this.fluidState = pFluidState;
    }

    public SChangeFluidPacket(IBlockReader pBlockGetter, BlockPos pPos) {
        this(pPos, pBlockGetter.getFluidState(pPos));
    }

    public SChangeFluidPacket(PacketBuffer pBuffer) {
        this.pos = pBuffer.readBlockPos();
        this.fluidState = Fluid.FLUID_STATE_REGISTRY.byId(pBuffer.readVarInt());
    }

    public void encode(PacketBuffer pBuffer) {
        pBuffer.writeBlockPos(this.pos);
        pBuffer.writeVarInt(FluidMixinInterface.getId(this.fluidState)); // create getId in FluidMixinInterface
    }

    public static SChangeFluidPacket decode(PacketBuffer pBuffer) {
        return new SChangeFluidPacket(pBuffer);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                // Make sure it's only executed on the physical client
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        ClientWPONetHandler.handleFluidUpdate(this, ctx))
        );
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    public FluidState getFluidState() {
        return this.fluidState;
    }

    @OnlyIn(Dist.CLIENT)
    public BlockPos getPos() {
        return this.pos;
    }
}
