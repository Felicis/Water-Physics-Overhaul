package net.skds.wpo.util.marker;

import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;

public interface WPOFluidMarker {
    public static boolean isWPOFluid(Block block)
    {
        return block instanceof WPOFluidMarker;
    }
    public static boolean isWPOFluid(Fluid fluid)
    {
        return fluid instanceof WPOFluidMarker;
    }
}