package net.skds.wpo.util.marker;

import net.minecraft.block.Block;

public interface WPOFluidMarker {
    public static boolean isWPOFluid(Block block)
    {
        return block instanceof WPOFluidMarker;
    }
}