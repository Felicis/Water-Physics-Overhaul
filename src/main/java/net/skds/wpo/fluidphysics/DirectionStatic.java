package net.skds.wpo.fluidphysics;

import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

public class DirectionStatic {
    public static Direction dirFromVec(BlockPos pos, BlockPos pos2) {
        return Direction.getNearest(pos2.getX() - pos.getX(), pos2.getY() - pos.getY(),
                pos2.getZ() - pos.getZ());
    }
}
