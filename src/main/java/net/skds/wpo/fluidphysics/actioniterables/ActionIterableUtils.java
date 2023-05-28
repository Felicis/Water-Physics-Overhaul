package net.skds.wpo.fluidphysics.actioniterables;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ActionIterableUtils {
    public static void fillStates(Long2ObjectLinkedOpenHashMap<BlockState> states, World world) {
        if (!world.isClientSide) {
            states.forEach((lpos, state) -> {
                world.setBlockAndUpdate(BlockPos.of(lpos), state);
            });
        }
    }
}
