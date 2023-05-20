package net.skds.wpo.util;

import net.minecraft.block.Block;

public interface AutomaticFluidloggableMarker {
    public static boolean shouldAddProperties(Block block)
    {
        return block instanceof AutomaticFluidloggableMarker;
    }
}
