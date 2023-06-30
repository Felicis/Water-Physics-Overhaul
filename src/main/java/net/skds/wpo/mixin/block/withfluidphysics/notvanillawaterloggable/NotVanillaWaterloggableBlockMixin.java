package net.skds.wpo.mixin.block.withfluidphysics.notvanillawaterloggable;

import net.minecraft.block.*;
import net.skds.wpo.util.marker.WPOFluidloggableMarker;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({
        // all blocks that do not fill the cube (could contain water) EXCLUDING IWaterLoggable
        // logically NOT waterloggable (imo): commented out
        // - small plants (crops, flowers, bushes)
        // - almost full blocks, where the water level would not be visible (e.g. grass path, enchanting table)
        // - overworld fire-based (torch, fire, campfire->only w/o fire)
        // logically waterloggable (imo):
        // - redstone stuffs (torches, dust, rails, comparator/repeater)
        // - soul fire (fire, torches, campfire)
        // - large plants (chorus flower, leaves, bamboo)
        // - aquatic plants and blocks
        FlowerPotBlock.class, // TODO: maybe expel content when filled with water? (if already in water maybe do not allow planting?)
        AbstractPressurePlateBlock.class, // WeightedPressurePlateBlock, PressurePlateBlock
        RedstoneBlock.class,
        ChorusFlowerBlock.class,
//        TorchBlock.class, // RedstoneTorchBlock(RedstoneWallTorchBlock), WallTorchBlock
        RedstoneTorchBlock.class, // RedstoneWallTorchBlock
//        WallTorchBlock.class,
        SixWayBlock.class, // ChorusPlantBlock
        HorizontalFaceBlock.class, // AbstractButtonBlock(Wood, Stone), GrindstoneBlock, LeverBlock
        RedstoneDiodeBlock.class, // ComparatorBlock, RepeaterBlock
        FenceGateBlock.class,
        BedBlock.class, // TODO: bed destroys water!
        CocoaBlock.class,
        StonecutterBlock.class,
//        EndPortalFrameBlock.class,
        DoorBlock.class,
        BambooSaplingBlock.class,
//        CactusBlock.class, // is blocking water: is that okay?
        AbstractRailBlock.class, // DetectorRailBlock, RailBlock, PoweredRailBlock
//        VineBlock.class, // TODO: break when touching water, not disappear
//        AbstractPlantBlock.class, // *Vines & Kelp
//        SugarCaneBlock.class, // TODO: break when touching water, not disappear
//        SnowBlock.class, // TODO: when destroy, create water? Also transform snowy dirt into normal dirt/grass
        SoulFireBlock.class, // TODO: eats water?!
//        FireBlock.class, //
        WebBlock.class,
        LeavesBlock.class,
//        CakeBlock.class, // TODO: should break in contact with water
        DragonEggBlock.class,
        AnvilBlock.class,
        RedstoneWireBlock.class, // TODO: destroys water (redstone updates?)
        EndRodBlock.class,
//        PistonHeadBlock.class, // because we want to be able to push water
        CarpetBlock.class,
        AbstractSkullBlock.class, // wall/~, player/witherskel
//        BrewingStandBlock.class,
        AbstractBannerBlock.class,
//        EnchantingTableBlock.class, // XXXX too little air in shape
        HopperBlock.class,
        DaylightDetectorBlock.class,
        LecternBlock.class,
//        MovingPistonBlock.class, // ???
        BellBlock.class,
//        DeadBushBlock.class,
//        TallGrassBlock.class,
        FlowerBlock.class, // WitherRoseBlock
//        AttachedStemBlock.class,
        NetherRootsBlock.class,
//        FungusBlock.class,
//        CropsBlock.class, // Potato, Carrot, Beetroot
//        TallFlowerBlock.class
        TallSeaGrassBlock.class, // TODO: should break when not waterlogged (contains empty fluid?); growing seagrass destroys water
//        NetherSproutsBlock.class, // ~~~ nether grass
//        SweetBerryBushBlock.class,
//        NetherWartBlock.class,
//        MushroomBlock.class,
        SeaGrassBlock.class, // TODO: should break when not waterlogged
//        StemBlock.class,
//        LilyPadBlock.class, // TODO should break when water level goes down
//        SaplingBlock.class,
//        TurtleEggBlock.class, // TODO should break
        TripWireHookBlock.class,
        TripWireBlock.class,
        BambooBlock.class,
//        CauldronBlock.class, // TODO: should not break in contact with water
})
public abstract class NotVanillaWaterloggableBlockMixin implements IWaterLoggable, WPOFluidloggableMarker {
    /************+ MARKER MIXIN: do not remove this mixing even if empty ****************/
}


