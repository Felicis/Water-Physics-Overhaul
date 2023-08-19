package net.skds.wpo.util;

import net.minecraft.block.Block;

public interface WPOFluidloggableMarker {
    public static boolean isWPOFluidloggable(Block block)
    {
        return block instanceof WPOFluidloggableMarker;
    }
}
