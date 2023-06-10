package net.skds.wpo.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraftforge.fml.network.NetworkEvent;
import net.skds.wpo.mixininterfaces.ClientWorldMixinInterface;
import net.skds.wpo.mixininterfaces.IWorldWriterMixinInterface;

import java.util.function.Supplier;

public class ClientWPONetHandler {
    // code adapted from ClientPlayNetHandler#handleBlockUpdate and #handleChunkBlocksUpdate
    public static void handleFluidUpdate(SChangeFluidPacket pPacket, Supplier<NetworkEvent.Context> ctx) {
        ClientWorld level = Minecraft.getInstance().level;
        ((ClientWorldMixinInterface) level).setKnownState(pPacket.getPos(), pPacket.getFluidState()); // cast
    }

    public static void handleChunkFluidsUpdate(SMultiFluidChangePacket pPacket, Supplier<NetworkEvent.Context> ctx) {
        ClientWorld level = Minecraft.getInstance().level;
        pPacket.runUpdates((blockPos, fluidState) -> {
            ((IWorldWriterMixinInterface) level).setFluid(blockPos, fluidState, 19); // cast
        });
    }
}
