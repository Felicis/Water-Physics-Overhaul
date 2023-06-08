package net.skds.wpo.mixin.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.util.IStringSerializable;
import net.minecraft.world.gen.Heightmap;
import net.skds.wpo.fluidphysics.FFluidStatic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(Heightmap.Type.class)
public abstract class Heightmap$TypeMixin implements IStringSerializable {
    @Shadow
    @Final
    private String serializationKey;

    @Mutable
    @Shadow
    @Final
    private Predicate<BlockState> isOpaque; // has to be mutable to override

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void initM(CallbackInfo ci) {
        // enum constructor (RETURN injection guarantees, that `this` is initialized)
        // UPGRADE: fix lambda predicates for enums "MOTION_BLOCKING" and "MOTION_BLOCKING_NO_LEAVES"
        // replace BlockState.getFluidState() with FFluidStatic.getDefaultFluidState(BlockState) to get "sensible" fluid state from block state
        // TODO check if okay:
        //  if blockstate is
        //      1) not fluidblock,
        //      2) not motionblocking, and
        //      3) currently waterlogged with liquid (partially or full)
        //  it will incorrectly return Fluids.EMPTY.
        //  Examples where this is used:
        //  - spawning locations
        //  - raining and snowing mechanics (?)
        //  Examples of blockstates that fulfil the above criteria (check Material.java):
        //  - (waterlogged) bamboo shoots
        //  - (waterlogged) fire
        //  - (waterlogged) webs
        //  - (waterlogged) (water) plants
        //  - (waterlogged) cloth decorations
        if ("MOTION_BLOCKING".equals(this.serializationKey)) {
            this.isOpaque = blockState -> (blockState.getMaterial().blocksMotion()
                    || !FFluidStatic.getDefaultFluidState(blockState).isEmpty()) && !(blockState.getBlock() instanceof LeavesBlock);
        } else if ("MOTION_BLOCKING_NO_LEAVES".equals(this.serializationKey)) {
            this.isOpaque = blockState -> blockState.getMaterial().blocksMotion() || !FFluidStatic.getDefaultFluidState(blockState).isEmpty();
        }
    }
}
