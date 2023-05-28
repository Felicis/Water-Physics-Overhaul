package net.skds.wpo.mixin;

import net.minecraft.block.IWaterLoggable;
import net.skds.wpo.util.marker.WPOFluidloggableMarker;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(IWaterLoggable.class)
public interface IWaterLoggableMixin extends WPOFluidloggableMarker { // TODO to override IWaterLoggable methods patch separately
    /************+ MARKER MIXIN: do not remove this mixing even if empty ****************/
}
//    AbstractCoralPlantBlock.class, // CoralFanBlock(CoralFinBlock,DeadCoralWallFanBlock(CoralWallFanBlock)),CoralPlantBlock, DeadCoralPlantBlock
//    AbstractSignBlock.class, // Wall, Standing
//    CampfireBlock.class, TODO: put out when water flows into it (optional: keep soul campfires lit - same block!)
//    ChainBlock.class,
//    ChestBlock.class, // ~~~ only makes sense because of visual border
//    ConduitBlock.class,
//    EnderChestBlock.class, // ~~~ only makes sense because of visual border
//    FourWayBlock.class, // FenceBlock, PaneBlock
//    LadderBlock.class,
//    LanternBlock.class,
//    ScaffoldingBlock.class,
//    SeaPickleBlock.class,
//    SlabBlock.class, TODO: when become double slab => expel water! (double slab already does not allow water to flow through)
//    StairsBlock.class,
//    TrapDoorBlock.class,
//    WallBlock.class,
