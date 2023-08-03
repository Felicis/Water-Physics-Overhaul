package net.skds.wpo.fluidphysics;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.material.Material;
import net.minecraft.fluid.*;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.BlockFlags;
import net.skds.wpo.WPO;
import net.skds.wpo.WPOConfig;
import net.skds.wpo.fluidphysics.actioniterables.Equalizer;
import net.skds.wpo.fluidphysics.actioniterables.FluidDisplacer;
import net.skds.wpo.fluidphysics.actioniterables.LedgeFinder;
import net.skds.wpo.mixininterfaces.WorldMixinInterface;
import net.skds.wpo.util.Constants;
import net.skds.wpo.util.marker.WPOFluidMarker;
import net.skds.wpo.util.marker.WPOFluidloggableMarker;
import net.skds.wpo.util.tuples.Tuple2;
import net.skds.wpo.util.tuples.Tuple3;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static net.minecraft.state.properties.BlockStateProperties.WATERLOGGED;

public class FFluidStatic {


    // ================ UTIL ================== //

    /**
     * get list of down, randomized horizontal directions, up
     *
     * @param r
     * @return
     */
    public static List<Direction> getDirsDownRandomHorizontalUp(Random r) {
        List<Direction> randomDirs = getDirsRandomHorizontal(r);
        randomDirs.add(0, Direction.DOWN);
        randomDirs.add(Direction.UP);
        return randomDirs;
    }

    /**
     * get list of down, randomized horizontal directions (not up)
     *
     * @param r
     * @return
     */
    public static List<Direction> getDirsDownRandomHorizontal(Random r) {
        List<Direction> randomDirs = getDirsRandomHorizontal(r);
        randomDirs.add(0, Direction.DOWN);
        return randomDirs;
    }

    /**
     * get list of up, randomized horizontal directions, down
     *
     * @param r
     * @return
     */
    public static List<Direction> getDirsUpRandomHorizontalDown(Random r) {
        List<Direction> randomDirs = getDirsRandomHorizontal(r);
        randomDirs.add(0, Direction.UP);
        randomDirs.add(Direction.DOWN);
        return randomDirs;
    }

    /**
     * get list of horizontal randomized directions
     *
     * @param r
     * @return
     */
    public static List<Direction> getDirsRandomHorizontal(Random r) {
        List<Direction> directionList = Direction.Plane.HORIZONTAL.stream().collect(Collectors.toList());
        Collections.shuffle(directionList);
        return directionList;
    }

    public static Direction[] getRandomizedDirections(Random r, boolean addVertical) {

        Direction[] dirs = new Direction[4];

        if (addVertical) {
            dirs = new Direction[6];
            dirs[4] = Direction.DOWN;
            dirs[5] = Direction.UP;
        }
        int i0 = r.nextInt(4);
        for (int index = 0; index < 4; ++index) {
            Direction dir = Direction.from2DDataValue((index + i0) % 4);
            dirs[index] = dir;
        }

        return dirs;
    }

    public static Direction[] getAllRandomizedDirections(Random r) {

        Direction[] dirs = new Direction[6];

        int i0 = r.nextInt(6);
        for (int index = 0; index < 6; ++index) {
            Direction dir = Direction.from3DDataValue((index + i0) % 6);
            dirs[index] = dir;
        }

        return dirs;
    }

    public static FluidState getDefaultFluidState(BlockState blockState) {
        if (blockState.getBlock() instanceof FlowingFluidBlock) { // if is fluid block...
            return ((FlowingFluidBlock) blockState.getBlock()).getFluid().defaultFluidState(); // ... get default fluid state for it
        } else {
            // TODO: auto-waterlog for worldgen
            return Fluids.EMPTY.defaultFluidState(); // else return empty
        }
    }

    public static boolean isEmpty(Fluid fluid) {
        // DO NOT CALL state.getFluidState() => recursion
        // replaces: protected Fluid.isEmpty()
        return fluid.isSame(Fluids.EMPTY);
    }

    /**
     * returns true is block is air (abstraction around forge air block)<br/>
     * use to check for empty (water accepting) locations
     *
     * @param state
     * @return
     */
    public static boolean isAirBlock(BlockState state) {
        // use forge positional isAir(World,BlockPos) over isAir(), although the parameters are not used (set to null)
        // see: https://github.com/MinecraftForge/MinecraftForge/issues/7409
        return state.isAir(null, null);
    }

    /**
     * returns true if state is a FluidBlock and is modded by WPO to have custom leveling<br/>
     * use to check for pure fluid blocks (to be handled by WPO)
     *
     * @param state
     * @return
     */
    public static boolean isFluidBlock(BlockState state) {
        return WPOFluidMarker.isWPOFluid(state.getBlock());
    }

    /**
     * returns true if state is a "normal" Block and is modded by WPO to have custom leveling<br/>
     * use to check for fluidloggable blocks (to be handled by WPO)
     *
     * @param state
     * @return
     */
    public static boolean isFluidloggableBlock(BlockState state) {
        return WPOFluidloggableMarker.isWPOFluidloggable(state.getBlock());
    }

    /**
     * checks whether block could contain fluid (with exceptions), but not whether there is space for new fluid
     * <br/>
     * This is the case when:<br/>
     * a) the block is air (can be replaced with fluid)<br/>
     * a) the block is a (pure) fluid block<br/>
     * b) the block is a "normal" block extended by WPO to be fluidlogged<br/><br/>
     * This does not guarantee that there is enough space left in the pos, but it can always be ejected :)
     *
     * @param state
     * @return
     */
    public static boolean canHoldFluid(BlockState state) { // TODO fix (maybe check FlowingFluid.canHoldFluid)
        if (isAirBlock(state)) // can be replaced with fluid block
            return true;
        if (isFluidBlock(state)) {
            return true;
        } else if (isFluidloggableBlock(state)) { // WPO fluidloggable (not only WATERLOGGED, but also)
            // exceptions of fluidloggable blocks that should not hold fluid:
            if (state.getBlock() instanceof SlabBlock && state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE) {
                return false; // double slabs cannot hold fluid
            }
            return true;
        } else { // can not hold fluid: check if can be destroyed (?)
            // UGPRADE check FlowingFluid.canHoldFluid => if can be destroyed by fluid?
            Material material = state.getMaterial();
            if (material != Material.PORTAL && material != Material.STRUCTURAL_AIR && material != Material.WATER_PLANT && material != Material.REPLACEABLE_WATER_PLANT) {
                return !material.blocksMotion();
            } else {
                return false;
            }
        }
    }

    /**
     * getter for WATERLOGGED property (only access through this!)
     *
     * @param state
     * @return
     */
    public static boolean isFluidlogged(BlockState state) { // TODO check FluidState (add level and pos args)
        return state.getValue(WATERLOGGED);
    }

    /**
     * setter for WATERLOGGED property (only access through this!)
     *
     * @param state
     * @param fluidlogged
     * @return
     */
    public static BlockState setFluidlogged(BlockState state, boolean fluidlogged) { // TODO remove? set fluidState instead
        return state.setValue(WATERLOGGED, fluidlogged);
    }

//	/**
//	 * getter for WPO_LEVEL property (only access through this!)
//	 * @param state
//	 * @return
//	 */
//	public static int getFluidBlockLevel(BlockState state){
//		return state.getValue();
//	}

//	/**
//	 * setter for WPO_LEVEL property (only access through this!)
//	 * @param state
//	 * @param level
//	 * @return
//	 */
//	public static BlockState setFluidLevel(BlockState state, int level){ // TODO update FluidState (only update BlockState if FlowingFluidBlock - legacyLevel)
//		if (level < 0 || level > Constants.MAX_FLUID_LEVEL) {
//			throw new RuntimeException("Incorrect fluid level!!!");
//		} else {
//			return state.setValue(WPO_LEVEL, level);
//		}
//	}

//	/**
//	 * getter for WPO_FLUID property (only access through this!)
//	 * @param state
//	 * @return
//	 */
//	public static Fluid getFluid(BlockState state){ // TODO remove or add level, pos & get FluidState
//		return property2Fluid(state.getValue(WPO_FLUID));
//	}

//	/**
//	 * setter for WPO_FLUID (only access through this!)
//	 * @param state
//	 * @param fluid
//	 * @return
//	 */
//	public static BlockState setFluid(BlockState state, Fluid fluid){ // TODO remove or add level, pos & set FluidState
//		return state.setValue(WPO_FLUID, fluid2Property(fluid));
//	}

    private static FluidState getSourceOrFlowing(FlowingFluid fluid, int fluidLevel) {
        if (fluidLevel == 8) { // source
            return fluid.getSource(false);
        } else { // flowing
            return fluid.getFlowing(fluidLevel, false);
        }
    }

    public static FluidState getSourceOrFlowingOrEmpty(FlowingFluid fluid, int fluidLevel) {
        if (fluidLevel >= 8) { // source
            return fluid.getSource(false);
        }
        if (fluidLevel <= 0) { // empty
            return Fluids.EMPTY.defaultFluidState();
        } else { // flowing
            return fluid.getFlowing(fluidLevel, false);
        }
    }

    /**
     * extracts up to maxLevelsToTake levels from given fluidState. returns how many levels taken and fluidState with levels removed
     *
     * @param fluidState
     * @param maxLevelsToTake
     * @return
     */
    public static Tuple2<Integer, FluidState> takeLevelsUpTo(FluidState fluidState, FlowingFluid fluidToPlace, int maxLevelsToTake) {
        int actuallyTaken = Math.min(maxLevelsToTake, fluidState.getAmount());
        int newFSLevel = fluidState.getAmount() - actuallyTaken;
        FluidState modifiedFluidState = getSourceOrFlowingOrEmpty(fluidToPlace, newFSLevel);
        return new Tuple2<>(actuallyTaken, modifiedFluidState);
    }

    /**
     * places up to maxLevelsToPlace levels of given fluid into given fluidState
     *
     * @param fluidState
     * @param maxLevelsToPlace
     * @return 1st: whether levels were placed, 2nd: the # of placed levels, 3rd: the modified FS with levels placed
     */
    public static Tuple3<Boolean, Integer, FluidState> placeLevelsUpTo(FluidState fluidState, FlowingFluid fluidToPlace, int maxLevelsToPlace) {
        if (!fluidState.isEmpty() && !fluidState.getType().isSame(fluidToPlace)) { // wrong fluid => place nothing
            return new Tuple3<>(false, 0, fluidState);
        }
        int freeSpaceInFluidState = Constants.MAX_FLUID_LEVEL - fluidState.getAmount();
        int actuallyPlaced = Math.min(maxLevelsToPlace, freeSpaceInFluidState);
        int newFSLevel = fluidState.getAmount() + actuallyPlaced;
        FluidState modifiedFluidState = getSourceOrFlowingOrEmpty(fluidToPlace, newFSLevel);
        return new Tuple3<>(fluidState != modifiedFluidState, actuallyPlaced, modifiedFluidState);
    }

    private static BlockState updateFromFluidState(BlockState oldBlockState, FluidState newFluidState) {
        if (isAirBlock(oldBlockState) || isFluidBlock(oldBlockState)) { // was air or fluid
            return newFluidState.createLegacyBlock(); // create blockstate from fluid
        } else if (isFluidloggableBlock(oldBlockState)) {
            return setFluidlogged(oldBlockState, !newFluidState.isEmpty());
        } else { // other blocks => vanilla behaviour
            return oldBlockState;
        }
    }

    public static boolean setBlockAndFluid(World world, BlockPos pos, BlockState blockState, FluidState fluidState, boolean displaceFluid) {
        return setBlockAndFluid(world, pos, blockState, fluidState, displaceFluid, 3);
    }

    public static boolean setBlockAndFluid(World world, BlockPos pos, BlockState blockState, FluidState fluidState, boolean displaceFluid, int flags) {
        return setBlockAndFluid(world, pos, blockState, fluidState, displaceFluid, flags, 512);
    }

    /**
     * tries setting blockstate and fluidstate for pos. adjusts blockstate if needed (waterlogged, fluidblock level).
     * Displaces fluid if needed. If block does not accept fluid and fluid can not be displaced, delete fluid.
     *
     * @param world
     * @param pos
     * @param blockState
     * @param fluidState
     * @param displaceFluid
     * @param flags
     * @param recursion
     * @return
     */
    public static boolean setBlockAndFluid(World world, BlockPos pos, BlockState blockState, FluidState fluidState, boolean displaceFluid, int flags, int recursion) {
        if (canHoldFluid(blockState)) { // can hold fluid => set fluid and (fluid-updated) block
            BlockState newBlockState = updateFromFluidState(blockState, fluidState);
            boolean setBlockSuccess = ((WorldMixinInterface) world).setBlockNoFluid(pos, newBlockState, flags);
            boolean setFluidSuccess = ((WorldMixinInterface) world).setFluid(pos, fluidState, flags);
            return setBlockSuccess && setFluidSuccess;
        } else if (false) {
            // TODO if block should be destroyed on fluid contact:
            //  - World.destroyBlock
            //  - FluidUtil.destroyBlockOnFluidPlacement
            //  - BucketItem.emptyBucket
            return false;
        } else if (!displaceFluid) { // can NOT hold fluid and DONT displace => destroy fluid
            boolean setBlockSuccess = ((WorldMixinInterface) world).setBlockNoFluid(pos, blockState, flags);
            boolean setFluidSuccess = ((WorldMixinInterface) world).setFluid(pos, Fluids.EMPTY.defaultFluidState(), flags);
            return setBlockSuccess && setFluidSuccess;
        } else if (recursion > 0) { // can not hold fluid => try displacing (prevent infinite recursion), then place block
            FluidDisplacer displacer = new FluidDisplacer(world, pos, fluidState);
            // flags = 3 is okay (block and client update + rerender + neighbor changes)
            boolean setFluidSuccess = displacer.tryExecute(3, recursion - 1); // prevent infinite recursion
            boolean setBlockSuccess = ((WorldMixinInterface) world).setBlockNoFluid(pos, blockState, flags); // first displace fluid, then set block
            return setBlockSuccess && setFluidSuccess;
        } else { // recursion limit reached: error message with stack trace (but no exception)
            WPO.LOGGER.error("FFluidStatic.setBlockAndFluid and FluidDisplacer: reached recursion limit!", new Throwable());
            return false;
        }
    }

    public static boolean setBlockAlsoFluid(World world, BlockPos pos, BlockState blockState, boolean displaceFluid) {
        return setBlockAlsoFluid(world, pos, blockState, displaceFluid, 3, 512);
    }

    public static boolean setBlockAlsoFluid(World world, BlockPos pos, BlockState blockState, boolean displaceFluid, int flags) {
        return setBlockAlsoFluid(world, pos, blockState, displaceFluid, flags, 512);
    }

    public static boolean setBlockAlsoFluid(World world, BlockPos pos, BlockState blockState, boolean displaceFluid, int flags, int recursion) {
        FluidState fluidState = world.getFluidState(pos);
        if (fluidState.isEmpty()) { // TODO schedule fluid tick for neighbors?
            boolean setBlockSuccess = ((WorldMixinInterface) world).setBlockNoFluid(pos, blockState, flags, recursion);
            boolean setFluidSuccess = ((WorldMixinInterface) world).setFluid(pos, Fluids.EMPTY.defaultFluidState(), flags, recursion); // no recursion?
            return setBlockSuccess && setFluidSuccess;
        } else { // UPGRADE: if setBlock is called with moving but not set again later, call setBlockAndFluid in mixin to adapt to fluid
            if ((flags & BlockFlags.IS_MOVING) != 0) { // if MOVING => setBlock is also called later without MOVING => ignore fluids now
                return ((WorldMixinInterface) world).setBlockNoFluid(pos, blockState, flags, recursion);
            } else { // if not moving => adapt to fluid
                return setBlockAndFluid(world, pos, blockState, fluidState, displaceFluid, flags, recursion);
            }
        }
    }

    public static boolean setFluidAlsoBlock(World world, BlockPos pos, FluidState fluidState) {
        return setFluidAlsoBlock(world, pos, fluidState, 3);
    }

    public static boolean setFluidAlsoBlock(World world, BlockPos pos, FluidState fluidState, int flags) {
        return setFluidAlsoBlock(world, pos, fluidState, 3, 512);
    }

    public static boolean setFluidAlsoBlock(World world, BlockPos pos, FluidState fluidState, int flags, int recursion) {
        BlockState blockState = world.getBlockState(pos);
        return setBlockAndFluid(world, pos, blockState, fluidState, false, flags, recursion); // only changing fluid can not displace
    }

    public static boolean removeFluidAlsoBlock(World world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        return setBlockAndFluid(world, pos, blockState, Fluids.EMPTY.defaultFluidState(), false);
    }

//	/**
//	 * returns whether state contains fluid (which requires special fluid handling) or is empty
//	 * @param state
//	 * @return
//	 */
//	public static boolean containsFluid(BlockState state){ // TODO level, pos -> FluidState
//		if (isAirBlock(state)){
//			return false; // by definition does not contain fluid
//		} else if (isFluidBlock(state)){
//			return true; // by definition contains fluid
//		} else if (isFluidloggableBlock(state)){ // fluidloggable block can contain fluid or be empty
//			return !state.getFluidState().isEmpty(); // enforced by getFluidState Mixins
//		} else { // no fluid
//			return false;
//		}
//	}

//	/**
//	 * removes fluid from any blockstate (can hold fluid OR not) and returns it
//	 * @param state
//	 * @return
//	 */
//	public static BlockState removeFluid(BlockState state){  // TODO level, pos, FluidState?
//		if (isFluidBlock(state)){
//			state = setFluid(state, Fluids.EMPTY);
//			state = setFluidLevel(state, 0);
//			return state;
//		} else if (isFluidloggableBlock(state)){
//			state = setFluid(state, Fluids.EMPTY);
//			state = setFluidLevel(state, 0);
//			state = setFluidlogged(state, false);
//			return state;
//		} else {
//			return state;
//		}
//	}

//	/**
//	 * eject fluid contained in fluidstate at pos
//	 * @param world
//	 * @param pos
//	 * @return whether ejecting succeeded or not
//	 */
//	public static boolean ejectFluid(World world, BlockPos pos) {
//		FluidState oldFluidState = world.getFluidState(pos);
//		if (oldFluidState.isEmpty()) { // if empty
//			return true; // succeeded at ejecting fluid :)
//		} else { // if contains fluid
//			// displace fluid contained in oldFluidState
//			FluidDisplacer displacer = new FluidDisplacer(world, oldFluidState);
//			return displacer.tryExecute(pos); // return success or not
//		}
//	}

//	/**
//	 * updates newState given oldState fluid:
//	 * if newState cant hold fluid, ejects, otherwise copies fluid from oldState to newState<br/>
//	 * (creates new fluid block when copying to air)
//	 * @param world
//	 * @param pos
//	 * @param oldState
//	 * @param newState
//	 * @return updated newState
//	 */
//	public static BlockState copyFluidOrEject(ServerWorld world, BlockPos pos, BlockState oldState, BlockState newState){ // TODO level, pos, FluidState?, setFluid?
//		if (!containsFluid(oldState)) { // if no fluid in oldState
//			return newState; // nothing to copy TODO: maybe we need to remove fluid
//		} else if (isAirBlock(newState)){ // if new block is air, create liquid block from old fluid
//			FlowingFluid flowingFluid = (FlowingFluid) getFluid(oldState); // safe cast, because oldState contains fluid
//			int fluidLevel = getFluidLevel(oldState);
//			FluidState fluidState = getSourceOrFlowing(flowingFluid, fluidLevel);
//			return fluidState.createLegacyBlock();
//		} else if (canHoldFluid(newState)){ // if can hold => apply
//			newState = setFluid(newState, getFluid(oldState));
//			newState = setFluidLevel(newState, getFluidLevel(oldState));
//			newState = setFluidlogged(newState, isFluidlogged(oldState));
//			return newState;
//		} else { // if newState can not hold => eject
//			return ejectFluid(world, pos, oldState, newState);
//		}
//	}

//	public static BlockState copyFluidOrEject(ServerWorld world, BlockPos pos, BlockState newState){ // TODO level, pos, FluidState?, setFluid?
//		BlockState oldState = world.getBlockState(pos);
//		return copyFluidOrEject(world, pos, oldState, newState);
//	}

//	/**
//	 * Applies newlevel and fluid to state (waterlogs new block or creates water block).<br/>
//	 * Does not check whether state can hold fluid!
//	 * <br/><br/>
//	 * Safe alternatives:
//	 * - {@link FFluidStatic#copyFluidOrEject(ServerWorld, BlockPos, BlockState, BlockState)}
//	 * Assumes that
//	 * @param newState
//	 * @param newLevel
//	 * @param newFlowingFluid
//	 * @return
//	 */
//	public static BlockState forceApplyFluid(BlockState newState, int newLevel, FlowingFluid newFlowingFluid) { // TODO level, pos, FluidState?
//		// from FlowingFluid.spreadTo(IWorld, BlockPos, BlockState, ~, FluidState) => force places fluid and destroys if needed
////			if (pBlockState.getBlock() instanceof ILiquidContainer) { // if waterloggable
////				((ILiquidContainer)pBlockState.getBlock()).placeLiquid(pLevel, pPos, pBlockState, pFluidState); // place waterlogged
////			} else {
////				if (!pBlockState.isAir()) {
////					this.beforeDestroyingBlock(pLevel, pPos, pBlockState); // if not air, destroy (water/forge fluid => drop, lava => fizz)
////				}
////				pLevel.setBlock(pPos, pFluidState.createLegacyBlock(), 3);
////			}
//
//		if (newLevel == 0) { // if no fluid (FlowingFluid is never empty)
//			return removeFluid(newState); // TODO: remove just for safety (remove when all action iterators fixed)
//		} else if (isAirBlock(newState)){ // if new block is air, create liquid block
//			FluidState fluidState = getSourceOrFlowing(newFlowingFluid, newLevel);
//			return fluidState.createLegacyBlock();
//		} else if (canHoldFluid(newState)){ // if can hold (fluid block or fluidlogged) => apply
////			newState = setFluid(newState, newFlowingFluid);
//			newState = setFluidLevel(newState, newLevel);
//			if (isFluidloggableBlock(newState)){ // if fluidloggable => set fluidlogged
//				newState = setFluidlogged(newState, true);
//			}
//			return newState;
//		} else {
//			WPO.LOGGER.error("FFluidStatic.forceApplyFluid: Tried to force-apply fluid=" +
//					newFlowingFluid.toString() + " level=" + newLevel +
//					" to BlockState=" + newState.toString() +
//					" which can not hold fluid and is not air!");
//			return newState;
//		}
//	}

    public static boolean isSameFluid(Fluid f1, Fluid f2) { // TODO replace f1, f2 with FlowingFluid and inline
        if (f1 == Fluids.EMPTY)
            return false;
        if (f2 == Fluids.EMPTY)
            return false;
        return f1.isSame(f2);
    }

    // ================ OTHER ================== //

    // ================= UTIL ================== //
    // TODO fix with FlowingFluid.canPassThroughWall()
    public static boolean canFlow(IBlockReader world, BlockPos fromPos, Direction inDirection) {
        BlockState state1 = world.getBlockState(fromPos);
        BlockState state2 = world.getBlockState(fromPos.relative(inDirection));
        if (state2.canOcclude() && !canHoldFluid(state2)) { // TODO canOcclude?
            return false;
        }
        VoxelShape voxelShape1 = state1.getCollisionShape(world, fromPos);
        VoxelShape voxelShape2 = state2.getCollisionShape(world, fromPos.relative(inDirection));
        if (voxelShape1.isEmpty() && voxelShape2.isEmpty()) {
            return true;
        }
        return !VoxelShapes.mergedFaceOccludes(voxelShape1, voxelShape2, inDirection);
    }

//	public static boolean canFlow(IBlockReader w, BlockPos fromPos, BlockPos toPos, BlockState state1, BlockState state2, Fluid fluid) {
//
//		Fluid f2 = state2.getFluidState().getType();
//		if (f2.isSame(fluid) && state1.getBlock() instanceof FlowingFluidBlock
//				&& state2.getBlock() instanceof FlowingFluidBlock) {
//			return true;
//		}
//
//		FluidPars fp2 = (FluidPars) ((IBlockExtended) state2.getBlock()).getCustomBlockPars().get(FluidPars.class);
//		FluidPars fp1 = (FluidPars) ((IBlockExtended) state1.getBlock()).getCustomBlockPars().get(FluidPars.class);
//		boolean posos = false;
//		if (fp1 != null) {
//			if (fp1.isPassable == 1) {
//				posos = true;
//			}
//		}
//		Direction dir = DirectionStatic.dirFromVec(fromPos, toPos);
//		if (fp2 != null) {
//			if (fp2.isPassable == 1) {
//				// System.out.println(state2);
//				return true;
//			} else if (fp2.isPassable == -1) {
//				return false;
//			}
//			if ((state2.getFluidState().isEmpty() || state1.getFluidState().canBeReplacedWith(w, fromPos, f2, dir))
//					&& fp2.isDestroyableBy(fluid))
//				return true;
//		}
//
//		if (state2.canOcclude() && !posos && !state2.hasProperty(WATERLOGGED)) {
//			return false;
//		}
//		if (!(fluid instanceof WaterFluid) && (state1.hasProperty(WATERLOGGED) || state2.hasProperty(WATERLOGGED))) {
//			return false;
//		}
//		VoxelShape voxelShape1 = state1.getCollisionShape(w, fromPos);
//		VoxelShape voxelShape2 = state2.getCollisionShape(w, toPos);
//		if ((voxelShape1.isEmpty() || posos) && voxelShape2.isEmpty()) {
//			return true;
//		}
//		return !VoxelShapes.mergedFaceOccludes(voxelShape1, voxelShape2, dir);
//	}

    // ================= MIXINS ==================//

    public static Vector3d getVel(IBlockReader w, BlockPos pos, FluidState fs) {

        Vector3d vel = new Vector3d(0, 0, 0);
        int level = fs.getAmount();
        Fluid fluid = fs.getType();
        BlockPos posu = pos.above();

        boolean flag = false;

        FluidState stateu = w.getFluidState(posu);

        if (canFlow(w, pos, Direction.UP) && !stateu.isEmpty()) {
            level += stateu.getAmount();
            flag = true;
        }

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos pos2 = pos.relative(dir);

            FluidState state2 = w.getFluidState(pos2);

            if (!state2.isEmpty() && canFlow(w, pos, dir)) {
                int lvl2 = state2.getAmount();
                if (flag) {
                    FluidState fs2u = w.getFluidState(pos2.above());
                    if (isSameFluid(fluid, fs2u.getType())) {
                        lvl2 += fs2u.getAmount();
                    }
                }
                int delta = level - lvl2;
                if (delta > 1 || delta < -1) {
                    Vector3i v3i = dir.getNormal();
                    vel = vel.add(v3i.getX() * delta, 0, v3i.getZ() * delta);
                }
            }
            // vel.multiply((double) 1D/n);
        }
        return vel.normalize();
    }

    /**
     * schedules fluid tick if fluid:<br/>
     * 1) is not empty<br/>
     * 2) chunk is generated (no cascading worldgen from fluids)<br/>
     * 3) does not already have scheduled tick
     *
     * @param world
     * @param pos
     */
    public static void scheduleFluidTick(IWorld world, BlockPos pos) {
        Fluid fluid = world.getFluidState(pos).getType();
        if (!fluid.isSame(Fluids.EMPTY)) { // only schedule tick if contains fluid
            if (world.hasChunkAt(pos)) { // fluids should not gen chunks (but if not ticking should tick once they're ticking again)
                if (!world.getLiquidTicks().hasScheduledTick(pos, fluid)) { // no duplicate ticks
                    world.getLiquidTicks().scheduleTick(pos, fluid, fluid.getTickDelay(world));
                    // ticking chunk check not needed, because not ticking chunks do not process their ticks until they start ticking again
                }
            }
        }
    }

    /**
     * only used by mixin into flowing fluid tick()
     *
     * @param world
     * @param pos
     * @param fluidState
     */
    public static void tickFlowingFluid(World world, BlockPos pos, FluidState fluidState) {
        if (!fluidState.isEmpty()) { // if empty, skip ticking
            if (!world.isClientSide) { // only on server
                // TODO if changing from other pos (update neighbors et al) => do not tick (check hasScheduledTick?)

//                // no placing water in nether
//                if (handleUltraWarmDimensions(world, pos, fluidState))
//                    return;
//
//                // fluid mixing
//                if (handleFluidMixing(world, pos, fluidState))
//                    return;
//
                // find the closest ledge & move towards ledge
//                if (handleDownhillFlow(world, pos, fluidState))
//                    return;
                handleDownhillFlow(world, pos, fluidState);

                // do normal equalization
                if (handleEqualization(world, pos, fluidState, (FlowingFluid) fluidState.getType())) // safe cast because fluidstate not empty
                    return; // TODO: move before downhill check and also flow downhill

                // TODO check FFluidDefault/Basic/EQ
                // TODO (make action iterable?)
                // nop // TODO fix
////				1) only **send** fluid to neighbors (always 1 packet?) - receive from ticking neighbors with higher level
////				2) schedule tick for all neighbors if changed
////				3) if ledge: only send fluid towards ledge?
////				4) .... looks good?
                // TODO when flow triggered here call World.playLocalSound() -> ClientWorld.playLocalSound()... nop on server
                // make water/lava (forge fluid?) sounds when packets move (mixin to ClientWorld.setFluid?)
//			pLevel.playLocalSound((double)pPos.getX() + 0.5D, (double)pPos.getY() + 0.5D, (double)pPos.getZ() + 0.5D, SoundEvents.WATER_AMBIENT, SoundCategory.BLOCKS, pRandom.nextFloat() * 0.25F + 0.75F, pRandom.nextFloat() + 0.5F, false);
//			pLevel.playLocalSound((double)pPos.getX(), (double)pPos.getY(), (double)pPos.getZ(), SoundEvents.LAVA_AMBIENT, SoundCategory.BLOCKS, 0.2F + pRandom.nextFloat() * 0.2F, 0.9F + pRandom.nextFloat() * 0.15F, false);
            }
        }
    }

    private static boolean handleUltraWarmDimensions(World world, BlockPos pos, FluidState fluidState) {
        if (world.dimensionType().ultraWarm() && isWater(fluidState)) {
            int posX = pos.getX();
            int posY = pos.getY();
            int posZ = pos.getZ();
            world.playLocalSound(posX, posY, posZ, SoundEvents.FIRE_EXTINGUISH, SoundCategory.BLOCKS,
                    0.5F, 2.6F + (world.random.nextFloat() - world.random.nextFloat()) * 0.8F, false);

            for (int _i = 0; _i < 8; ++_i) {
                world.addParticle(ParticleTypes.LARGE_SMOKE, posX + Math.random(), posY + Math.random(), posZ + Math.random(),
                        0.0D, 0.0D, 0.0D);
            }
            return true;
        }
        return false;
    }

    private static boolean handleFluidMixing(World world, BlockPos pos, FluidState fluidState) {
        // only from lava perspective: works because setFluid of water schedules neighbor lava tick
        if (isLava(world.getFluidState(pos))) {
            // find water neighbor
            BlockPos neighborPos = null;
            boolean hasWaterNeighbor = false;
            for (Direction dir : getDirsDownRandomHorizontalUp(world.random)) {
                neighborPos = pos.relative(dir);
                if (canFlow(world, pos, dir) && isWater(world.getFluidState(neighborPos))) {
                    hasWaterNeighbor = true;
                    break;
                }
            }
            if (hasWaterNeighbor) { // neighborPos is water
                FluidState lavaFS = world.getFluidState(pos);
                FluidState waterFS = world.getFluidState(neighborPos);
                // constants
                FluidState emptyFluid = Fluids.EMPTY.defaultFluidState();
                BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
                BlockState cobblestone = Blocks.COBBLESTONE.defaultBlockState();
                BlockState stone = Blocks.STONE.defaultBlockState();
                // fluid mixing cases:
                if (lavaFS.isSource()) { // case 1: lava source + water (source or flowing) => obsidian
                    setBlockAndFluid(world, pos, obsidian, emptyFluid, false); // TODO drop old block if waterlogged
                } else { // flowing lava
                    if (waterFS.isSource()) { // case 2: lava flowing + water source => stone
                        setBlockAndFluid(world, pos, stone, emptyFluid, false); // TODO drop old block if waterlogged
                    } else { // case 3: lava flowing + water flowing => cobblestone
                        setBlockAndFluid(world, pos, cobblestone, emptyFluid, false); // TODO drop old block if waterlogged
                    }
                }
                // also remove water
                removeFluidAlsoBlock(world, neighborPos); // keep old block if waterlogged
                // TODO play lava and water hissing sounds
                return true;
                // TODO lava-water interaction FlowingFluidBlock.shouldSpreadLiquid
                // TODO lava-water interaction: check LavaFluid.spreadTo() -- config: #lava packets == 1 stone (maybe consume 1 or #lava water packets?)
            }
        }
        return false;
    }

    private static boolean handleDownhillFlow(World world, BlockPos pos, FluidState fluidState) {
        if (fluidState.isEmpty()) {
            return false;
        }
        LedgeFinder ledgeFinder = new LedgeFinder(world, pos);
        Tuple2<Boolean, Direction> successAndFlowDir = ledgeFinder.tryExecuteWithResult();
        if (successAndFlowDir.first) { // move towards ledge
//					FluidState fromFluidState = world.getFluidState(pos);
//					FluidState toFluidState = world.getFluidState(ledgeDirPos);
//					Tuple3<Boolean, Integer, FluidState> tuple3 = FFluidStatic.placeLevelsUpTo(toFluidState, fromFluidState.getAmount());
//					Boolean wasPlaced = tuple3.first;
//					Integer placedLevels = tuple3.second;
//					FluidState newFluidState = tuple3.third;
//					if (wasPlaced) { // levels were actually placed
//						 -= placedLevels;
//					}
            // check how many levels can be placed
            FlowingFluid fromFluid = (FlowingFluid) fluidState.getType(); // cast safe, because not empty
            int fromLevel = fluidState.getAmount();
            BlockPos toPos = pos.relative(successAndFlowDir.second);
            FluidState toState = world.getFluidState(toPos);
            if (fromLevel > toState.getAmount()) { // at least one higher => flow downhill
                Tuple3<Boolean, Integer, FluidState> tuple3 = FFluidStatic.placeLevelsUpTo(toState, fromFluid, fromLevel);
                Boolean wasPlaced = tuple3.first;
                Integer placedLevels = tuple3.second;
                FluidState newToState = tuple3.third;
                if (wasPlaced) { // levels were actually placed
                    // update fromPos and toPos
                    setFluidAlsoBlock(world, toPos, newToState);
                    FluidState newFromState = FFluidStatic.getSourceOrFlowingOrEmpty(fromFluid, fromLevel - placedLevels);
                    setFluidAlsoBlock(world, pos, newFromState);
                } // else do nothing
                // TODO play sound
                // TODO update fluids recursively up-flow if all moving to ledge? (keep packets together)
                return true;
            }
        }
        return false;
    }

    private static boolean handleEqualization(World world, BlockPos startPos, FluidState fluidState, FlowingFluid fluid) {
        // TODO get eq flow direction (set) instead of doing it in iterable
        Equalizer equalizer = new Equalizer(world, startPos, fluid);
        equalizer.tryExecute();
//        // we assume no ledges nearby
//        FluidState startFS = world.getFluidState(startPos);
//        List<FluidState> neighborFluidStates = new ArrayList<>();
//        // find valid neighbors
//        for (Direction dir : getDirsRandomHorizontal(world.random)) {
//            if (canFlow(world, startPos, dir)) {
//                BlockPos neighborPos = startPos.relative(dir);
//                FluidState neighborFS = world.getFluidState(neighborPos);
//                BlockState neighborBS = world.getBlockState(neighborPos);
//                if (FFluidStatic.canHoldFluid(neighborBS)) {
//                    if (startFS.getType().isSame(neighborFS.getType()) || neighborFS.isEmpty()) { // only same fluid or empty
//                        neighborFluidStates.add(neighborFS);
//                    }
//                }
//            }
//        }
//        // get fluid packet sum
//        int packetSum = startFS.getAmount();
//        for (FluidState fs : neighborFluidStates) {
//            packetSum += fs.getAmount();
//        }
//        // do packet math
//        int packetsEach = packetSum / (1 + neighborFluidStates.size());
//        int remainder = packetSum % packetsEach;
        return false;
    }

    private static boolean isWater(FluidState fluidState) {
        return fluidState.getType().isSame(Fluids.WATER) || fluidState.getType().isSame(Fluids.FLOWING_WATER);
    }

    private static boolean isLava(FluidState fluidState) {
        return fluidState.getType().isSame(Fluids.LAVA) || fluidState.getType().isSame(Fluids.FLOWING_LAVA);
    }

    /**
     * ONLY used for mixin. to schedule tick use Fluid.getTickDelay (already mixed in with this)
     *
     * @param oldRate
     * @return
     */
    public static int getTickDelay(int oldRate) {
        // vanilla => water: 5, lava: 30 (10 in nether)
        return (int) (oldRate * WPOConfig.COMMON.fluidTickRateScaling.get());
    }
}