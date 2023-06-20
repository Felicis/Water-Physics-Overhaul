package net.skds.wpo.mixin.world;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.crash.ReportedException;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ChunkHolder;
import net.minecraftforge.common.capabilities.CapabilityProvider;
import net.minecraftforge.common.extensions.IForgeWorld;
import net.skds.wpo.fluidphysics.FFluidStatic;
import net.skds.wpo.mixininterfaces.FluidMixinInterface;
import net.skds.wpo.mixininterfaces.FluidStateMixinInterface;
import net.skds.wpo.mixininterfaces.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;
import java.util.Random;

@Mixin(World.class)
public abstract class WorldMixin extends CapabilityProvider<World> implements WorldMixinInterface, IWorld, AutoCloseable, IForgeWorld {
    @Shadow
    @Final
    public boolean isClientSide;

    @Shadow
    public abstract boolean isDebug();

    @Shadow
    public abstract Chunk getChunkAt(BlockPos pPos);

    @Shadow
    public abstract FluidState getFluidState(BlockPos pPos);

    @Shadow @Final public Random random;

    @Shadow public abstract void neighborChanged(BlockPos pPos, Block pBlock, BlockPos pFromPos);

    @Shadow public abstract boolean destroyBlock(BlockPos pPos, boolean pDropBlock, @Nullable Entity pEntity, int pRecursionLeft);

    protected WorldMixin(Class<World> baseClass) {
        super(baseClass);
    }

    @Redirect(method = "setBlock(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlock(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;II)Z"))
    private boolean setBlock_AlsoFluid(World world, BlockPos pos, BlockState blockState, int pFlags, int pRecursionLeft) {
        // set blockstate adapted to fluidstate or displace fluid
        return FFluidStatic.setBlockAlsoFluid(world, pos, blockState, true, pFlags, pRecursionLeft); // UPGRADE mixin direct usages of this method
    }

    /**
     * use this from inside WPO fluid handling, when the blockstate is already adapted to the fluid.<br/>
     * Do not use setBlock directly
     * @param pPos
     * @param pNewState
     * @return
     */
    @Override
    public boolean setBlockNoFluid(BlockPos pPos, BlockState pNewState) {
        return ((World) (Object) this).setBlock(pPos, pNewState, 3, 512);
    }

    /**
     * use this from inside WPO fluid handling, when the blockstate is already adapted to the fluid.
     * @param pPos
     * @param pNewState
     * @param pFlags
     * @return
     */
    @Override
    public boolean setBlockNoFluid(BlockPos pPos, BlockState pNewState, int pFlags) {
        return ((World) (Object) this).setBlock(pPos, pNewState, pFlags, 512);
    }

    @Override
    public boolean setBlockNoFluid(BlockPos pPos, BlockState pNewState, int pFlags, int recursion) {
        return ((World) (Object) this).setBlock(pPos, pNewState, pFlags, recursion);
    }

    @Override
    public boolean containsAnyLiquid(AxisAlignedBB pBb) {
        return ((IWorldReaderMixinInterface) this).containsAnyLiquidM(pBb);
    }

    @Override // kinda superfluous override (copied from setBlock), but you never know
    public boolean setFluid(BlockPos pPos, FluidState pNewState, int pFlags) {
        return this.setFluid(pPos, pNewState, pFlags, 512);
    }
    @Override
    public boolean setFluid(BlockPos pPos, FluidState pFluidState, int pFlags, int pRecursionLeft) {
        // copied from World.setBlock
        if (World.isOutsideBuildHeight(pPos)) { // change: static method cannot/need not be shadowed
            return false;
        } else if (!this.isClientSide && this.isDebug()) {
            return false;
        } else {
            Chunk chunk = this.getChunkAt(pPos);

            pPos = pPos.immutable(); // Forge - prevent mutable BlockPos leaks
            // CHANGE skip BlockSnapshot creation (only used for forge events; maybe add onFluidPlace event in future...)

            FluidState oldFluidState = ((IChunkMixinInterface) chunk).setFluidState(pPos, pFluidState, (pFlags & 64) != 0);
            if (oldFluidState == null) { // failed or state unchanged
                // CHANGE skip BlockSnapshot check
                return false;
            } else {
                // CHANGE skip light check
                // CHANGE skip BlockSnapshot check if
                this.markAndNotifyFluid(pPos, chunk, oldFluidState, pFluidState, pFlags, pRecursionLeft); // send to clients
                return true;
            }
        }
    }

    public void setFluidsDirty(BlockPos pBlockPos, FluidState pOldState, FluidState pNewState) {
        // nop (for server; client has implementation)
    }

    // SKIPPED destroyFluid: destroyBlock handles block dropping, but fluids do not drop

    // SKIPPED destroyBlock->setBlock Redirect: handles fluids already correctly (block replaced with fluid block and fluidstate unchanged)

    /**
     * only runs when setFluidState successful: state correctly set (correct fluid) && state actually changed (updates needed)
     * for flags also check: {@link net.minecraftforge.common.util.Constants.BlockFlags}
     */
    public void markAndNotifyFluid(BlockPos pPos, @Nullable Chunk chunk, FluidState oldFluidState, FluidState newFluidState, int pFlags, int pRecursionLeft) {
        // copied from World.markAndNotifyBlock() with changes
        Fluid fluid = newFluidState.getType();
        FluidState currentFluidState = getFluidState(pPos);
        // removed useless braces
        if (currentFluidState == newFluidState) {
            // client => set dirty
            if (oldFluidState != currentFluidState) {
                this.setFluidsDirty(pPos, oldFluidState, currentFluidState);
            }

            // if BLOCK_UPDATE && (isServer || NO_RERENDER) && (isClient || chunk is TICKING or closer)
            // server => send update to clients
            if ((pFlags & 2) != 0 && (!this.isClientSide || (pFlags & 4) == 0) && (this.isClientSide || chunk.getFullStatus() != null && chunk.getFullStatus().isOrAfter(ChunkHolder.LocationType.TICKING))) {
                this.sendFluidUpdated(pPos, oldFluidState, newFluidState, pFlags); // send fluidstate change to clients (sets to be sent on next chunk tick)
            }

            // if NOTIFY_NEIGHBORS
            // calls ServerWorld.fluidUpdated() -> World.updateNeighborsAt() -> for neighbors: World.neighborChanged() -> FluidState.neighborChanged() -> Fluid.neighborChanged()
            if ((pFlags & 1) != 0) {
                ((IWorldMixinInterface) this).fluidUpdated(pPos, oldFluidState.getType());
                // TODO skip? (block variant only used for analog output signal/redstone)
            }

            // if UPDATE_NEIGHBORS
            // calls FluidState.updateNeighbourShapes() -> for neightbors: FluidState.updateShape() -> Fluid.updateShape()
            // TODO equalize through: a) add fluid tick OR b) updateNeighbors ?
            if ((pFlags & 16) == 0 && pRecursionLeft > 0) {
                int i = pFlags & -34; // -34 = 0b1101_1110 => drop: NOTIFY_NEIGHBORS + NO_NEIGHBOR_DROPS (keep UPDATE_NEIGHBORS)
                // CHANGE skip diagonal updates (fluid flows only through sides)
                // update state of each neighbor given this new state and sets new neighbor states
                ((FluidStateMixinInterface) (Object) newFluidState).updateNeighbourShapes(this, pPos, i, pRecursionLeft - 1);
                // CHANGE skip diagonal updates
            }

            // CHANGE: skip this.onFluidStateChange (orig: onBlockStateChange), because it only updates POI blockstates (afaik fluidstates are not POIs)
        }
    }

    /**
     * force removes fluid without asking or dropping or doing anything
     *
     * @param pPos
     * @param pIsMoving
     * @return
     */
    @Override
    public boolean removeFluid(BlockPos pPos, boolean pIsMoving) {
        // TODO also update blockstate (waterlogged?)
        return this.setFluid(pPos, Fluids.EMPTY.defaultFluidState(), 3 | (pIsMoving ? 64 : 0)); // maybe pass IS_MOVING flag
    }

    @Override
    public void updateNeighborsAt(BlockPos pPos, Fluid pFluid) {
        // CHANGE no forge event exists for fluid state update
        // randomize neighbor update order (first below, then random horizontal, then above)
        for (Direction dir : FFluidStatic.getDirsDownRandomHorizontalUp(random)) {
            this.neighborChanged(pPos.relative(dir), pFluid, pPos);
        }
        // why use fixed order ???
//        this.neighborChanged(pPos.west(), pFluid, pPos);
//        this.neighborChanged(pPos.east(), pFluid, pPos);
//        this.neighborChanged(pPos.below(), pFluid, pPos);
//        this.neighborChanged(pPos.above(), pFluid, pPos);
//        this.neighborChanged(pPos.north(), pFluid, pPos);
//        this.neighborChanged(pPos.south(), pFluid, pPos);
    }

    @Override
    public void neighborChanged(BlockPos pPos, Fluid pFluid, BlockPos pFromPos) {
        // copied from World.neighborChanged(~, Block, ~)
        if (!this.isClientSide) {
            FluidState fluidState = this.getFluidState(pPos);

            try {
                ((FluidStateMixinInterface) (Object) fluidState).neighborChanged((World) (Object) this, pPos, pFluid, pFromPos, false);
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.forThrowable(throwable, "Exception while updating neighbours");
                CrashReportCategory crashreportcategory = crashreport.addCategory("Block being updated");
                crashreportcategory.setDetail("Source block type", () -> {
                    try {
                        return String.format("ID #%s (%s // %s)", pFluid.getRegistryName(), ((FluidMixinInterface) pFluid).getDescriptionId(), pFluid.getClass().getCanonicalName());
                    } catch (Throwable throwable1) {
                        return "ID #" + pFluid.getRegistryName();
                    }
                });
                // <<< copied from: CrashReportCategory.populateBlockDetails(crashreportcategory, pPos, fluidState);
                if (fluidState != null) {
                    crashreportcategory.setDetail("Block", fluidState::toString);
                }
                crashreportcategory.setDetail("Block location", () -> CrashReportCategory.formatLocation(pPos));
                // >>>
                throw new ReportedException(crashreport);
            }
        }
    }

    @Override
    public boolean setFluidAndUpdate(BlockPos pPos, FluidState pState) {
        // copied from World.setBlockAndUpdate
        return this.setFluid(pPos, pState, 3);
    }

    @Override
    public void updateNeighborsAtExceptFromFacing(BlockPos pPos, Fluid pFluidType, Direction pSkipSide) {
        // copied from World.updateNeighborsAtExceptFromFacing(~, Block, ~)
        java.util.EnumSet<Direction> directions = java.util.EnumSet.allOf(Direction.class);
        directions.remove(pSkipSide);
        // skip forge event onNeighborNotify

        if (pSkipSide != Direction.WEST) {
            this.neighborChanged(pPos.west(), pFluidType, pPos);
        }

        if (pSkipSide != Direction.EAST) {
            this.neighborChanged(pPos.east(), pFluidType, pPos);
        }

        if (pSkipSide != Direction.DOWN) {
            this.neighborChanged(pPos.below(), pFluidType, pPos);
        }

        if (pSkipSide != Direction.UP) {
            this.neighborChanged(pPos.above(), pFluidType, pPos);
        }

        if (pSkipSide != Direction.NORTH) {
            this.neighborChanged(pPos.north(), pFluidType, pPos);
        }

        if (pSkipSide != Direction.SOUTH) {
            this.neighborChanged(pPos.south(), pFluidType, pPos);
        }
    }
}
