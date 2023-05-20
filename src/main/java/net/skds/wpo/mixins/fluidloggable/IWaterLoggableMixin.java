package net.skds.wpo.mixins.fluidloggable;

import net.minecraft.block.IWaterLoggable;
import net.skds.wpo.util.AutomaticFluidloggableMarker;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(IWaterLoggable.class)
public interface IWaterLoggableMixin extends AutomaticFluidloggableMarker {

}
//    SlabBlock.class, // TODO: when become double slab => expel water!
//    LanternBlock.class,
//    StairsBlock.class,
//    AbstractSignBlock.class, // Wall, Standing
//    AbstractCoralPlantBlock.class, // CoralFanBlock(CoralFinBlock,DeadCoralWallFanBlock(CoralWallFanBlock)),CoralPlantBlock, DeadCoralPlantBlock
//    SeaPickleBlock.class,
//    TrapDoorBlock.class,
//    LadderBlock.class,
//    WallBlock.class,
//    EnderChestBlock.class, // ~~~ only makes sense because of visual border
//    ScaffoldingBlock.class,
//    FourWayBlock.class, // FenceBlock, PaneBlock
//    ChestBlock.class, // ~~~ only makes sense because of visual border
//    ChainBlock.class,
//    CampfireBlock.class, // TODO: put out when water flows into it (optional: keep soul campfires lit - same block!)
