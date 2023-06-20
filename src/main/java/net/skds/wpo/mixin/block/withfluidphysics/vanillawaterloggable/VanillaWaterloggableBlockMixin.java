package net.skds.wpo.mixin.block.withfluidphysics.vanillawaterloggable;

import net.minecraft.block.*;
import net.minecraft.fluid.FluidState;
import net.skds.wpo.WPO;
import net.skds.wpo.fluidphysics.FFluidStatic;
import net.skds.wpo.util.marker.WPOFluidloggableMarker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({
        AbstractCoralPlantBlock.class, // CoralFanBlock(CoralFinBlock,DeadCoralWallFanBlock(CoralWallFanBlock)),CoralPlantBlock, DeadCoralPlantBlock
        AbstractSignBlock.class, // Wall, Standing
        CampfireBlock.class, //TODO: put out when water flows into it (optional: keep soul campfires lit - same block!)
        ChainBlock.class,
        ChestBlock.class, // ~~~ only makes sense because of visual border
        ConduitBlock.class,
        EnderChestBlock.class, // ~~~ only makes sense because of visual border
        FourWayBlock.class, // FenceBlock, PaneBlock
        LadderBlock.class,
        LanternBlock.class,
        ScaffoldingBlock.class,
        SeaPickleBlock.class,
        SlabBlock.class, // TODO: when become double slab => expel water! (double slab already does not allow water to flow through)
        StairsBlock.class,
        TrapDoorBlock.class,
        WallBlock.class,
})
public class VanillaWaterloggableBlockMixin implements IWaterLoggable, WPOFluidloggableMarker {
    /************+ MARKER MIXIN: do not remove this mixing even if empty ****************/


    /**
     * RoadBlock Injection to catch FluidState creation
     */
    @Inject(method = "getFluidState", at = @At(value = "HEAD"), cancellable = true)
    public void getFluidStateM(BlockState state, CallbackInfoReturnable<FluidState> cir) {
        WPO.LOGGER.error("<IWaterLoggable>.getFluidState() was called!!!!", new Throwable());
//		throw new Exception("<IWaterLoggable>.getFluidState() was called!!!!");
    }
}
