package net.skds.wpo.fluidphysics;

import net.minecraft.block.*;
import net.minecraft.fluid.*;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.skds.core.api.IBlockExtended;
import net.skds.wpo.WPO;
import net.skds.wpo.WPOConfig;
import net.skds.wpo.fluidphysics.actioniterables.*;
import net.skds.wpo.mixininterfaces.WorldMixinInterface;
import net.skds.wpo.util.Constants;
import net.skds.wpo.util.tuples.Tuple2;
import net.skds.wpo.util.marker.WPOFluidMarker;
import net.skds.wpo.util.marker.WPOFluidloggableMarker;
import net.skds.wpo.util.pars.FluidPars;
import net.skds.wpo.util.tuples.Tuple3;

import java.util.*;

import static net.minecraft.state.properties.BlockStateProperties.WATERLOGGED;
import static net.skds.wpo.registry.BlockStateProps.*;

public class FFluidStatic {

	public final static int MILLIBUCKETS_PER_LEVEL = 1000 / Constants.MAX_FLUID_LEVEL;


	// ================ UTIL ================== //

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

	public static boolean isEmpty(Fluid fluid){
		// DO NOT CALL state.getFluidState() => recursion
		// replaces: protected Fluid.isEmpty()
		return fluid.isSame(Fluids.EMPTY);
	}

	/**
	 * returns true is block is air (abstraction around forge air block)<br/>
	 * use to check for empty (water accepting) locations
	 * @param state
	 * @return
	 */
	public static boolean isAirBlock(BlockState state){
		// use forge positional isAir(World,BlockPos) over isAir(), although the parameters are not used (set to null)
		// see: https://github.com/MinecraftForge/MinecraftForge/issues/7409
		return state.isAir(null, null);
	}

	/**
	 * returns true if state is a FluidBlock and is modded by WPO to have custom leveling<br/>
	 * use to check for pure fluid blocks (to be handled by WPO)
	 * @param state
	 * @return
	 */
	public static boolean isFluidBlock(BlockState state){
		return WPOFluidMarker.isWPOFluid(state.getBlock());
	}

	/**
	 * returns true if state is a "normal" Block and is modded by WPO to have custom leveling<br/>
	 * use to check for fluidloggable blocks (to be handled by WPO)
	 * @param state
	 * @return
	 */
	public static boolean isFluidloggableBlock(BlockState state){
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
	 * @param state
	 * @return
	 */
	public static boolean canHoldFluid(BlockState state){ // TODO fix (maybe check FlowingFluid.canHoldFluid)
		if (isAirBlock(state)) // can be replaced with fluid block
			return true;
		if (isFluidBlock(state)){
			return true;
		} else if (isFluidloggableBlock(state)){ // WPO fluidloggable (not only WATERLOGGED, but also)
			// exceptions of fluidloggable blocks that should not hold fluid:
			if (state.getBlock() instanceof SlabBlock && state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE) {
				return false; // double slabs cannot hold fluid
			}
			return true;
		} else { // can not hold fluid
			return false;
		}
	}

	/**
	 * getter for WATERLOGGED property (only access through this!)
	 * @param state
	 * @return
	 */
	public static boolean isFluidlogged(BlockState state){ // TODO check FluidState (add level and pos args)
		return state.getValue(WATERLOGGED);
	}

	/**
	 * setter for WATERLOGGED property (only access through this!)
	 * @param state
	 * @param fluidlogged
	 * @return
	 */
	public static BlockState setFluidlogged(BlockState state, boolean fluidlogged){ // TODO remove? set fluidState instead
		return state.setValue(WATERLOGGED, fluidlogged);
	}

	/**
	 * getter for WPO_LEVEL property (only access through this!)
	 * @param state
	 * @return
	 */
	public static int getFluidBlockLevel(BlockState state){
		return state.getValue();
	}

	/**
	 * setter for WPO_LEVEL property (only access through this!)
	 * @param state
	 * @param level
	 * @return
	 */
	public static BlockState setFluidLevel(BlockState state, int level){ // TODO update FluidState (only update BlockState if FlowingFluidBlock - legacyLevel)
		if (level < 0 || level > Constants.MAX_FLUID_LEVEL) {
			throw new RuntimeException("Incorrect fluid level!!!");
		} else {
			return state.setValue(WPO_LEVEL, level);
		}
	}

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

	/**
	 *
	 * @param state
	 * @return
	 */
	public static FluidState getFluidState(BlockState state){ // TODO remove or integrate into ~.getDefaultFluidStat()
		if (isFluidBlock(state)){ // guarantees not empty fluid
			FlowingFluid flowingFluid = (FlowingFluid) getFluid(state); // safe cast, because empty fluid block does not exist
			int fluidLevel = getFluidLevel(state);
			return getSourceOrFlowing(flowingFluid, fluidLevel);
		} else if (isFluidloggableBlock(state)){
			Fluid fluid = getFluid(state);
			if (isEmpty(fluid)){
				return Fluids.EMPTY.defaultFluidState();
			} else { // not empty
				FlowingFluid flowingFluid = (FlowingFluid) fluid; // safe cast, because not empty
				int fluidLevel = getFluidLevel(state);
				return getSourceOrFlowing(flowingFluid, fluidLevel);
			}
		} else { // other blocks => vanilla behaviour
			return state.getBlock().getFluidState(state); // see AbstractBlock.AbstractBlockState#getFluidState()
		}
	}

	private static FluidState getSourceOrFlowing(FlowingFluid fluid, int fluidLevel){
		if (fluidLevel == 8){ // source
			return fluid.getSource(false);
		} else { // flowing
			return fluid.getFlowing(fluidLevel, false);
		}
	}

	private static FluidState getSourceOrFlowingOrEmpty(FlowingFluid fluid, int fluidLevel){
		if (fluidLevel == 8){ // source
			return fluid.getSource(false);
		} if (fluidLevel == 0){ // empty
			return Fluids.EMPTY.defaultFluidState();
		} else { // flowing
			return fluid.getFlowing(fluidLevel, false);
		}
	}

	/**
	 * extracts up to maxLevelsToTake levels from given fluidState. returns how many levels taken and fluidState with levels removed
	 * @param fluidState
	 * @param maxLevelsToTake
	 * @return
	 */
	public static Tuple2<Integer, FluidState> takeLevelsUpTo(FluidState fluidState, int maxLevelsToTake) {
		int actuallyTaken = Math.min(maxLevelsToTake, fluidState.getAmount());
		int newFSLevel = fluidState.getAmount() - actuallyTaken;
		FluidState modifiedFluidState = getSourceOrFlowingOrEmpty((FlowingFluid) fluidState.getType(), newFSLevel);
		return new Tuple2<>(actuallyTaken, modifiedFluidState);
	}

	/**
	 * places up to maxLevelsToPlace levels into given fluidState
	 * @param fluidState
	 * @param maxLevelsToPlace
	 * @return 1st: whether levels were placed, 2nd: the # of placed levels, 3rd: the modified FS with levels placed
	 */
	public static Tuple3<Boolean, Integer, FluidState> placeLevelsUpTo(FluidState fluidState, int maxLevelsToPlace) {
		int freeSpaceInFluidState = Constants.MAX_FLUID_LEVEL - fluidState.getAmount();
		int actuallyPlaced = Math.min(maxLevelsToPlace, freeSpaceInFluidState);
		int newFSLevel = fluidState.getAmount() + actuallyPlaced;
		FluidState modifiedFluidState = getSourceOrFlowingOrEmpty((FlowingFluid) fluidState.getType(), newFSLevel);
		return new Tuple3<>(fluidState != modifiedFluidState, actuallyPlaced, modifiedFluidState);
	}

	private static BlockState updateFromFluidState(BlockState oldBlockState, FluidState newFluidState) {
		if (isFluidBlock(oldBlockState)){ // was fluid
			return newFluidState.createLegacyBlock(); // create blockstate from fluid
		} else if (isFluidloggableBlock(oldBlockState)){
			return setFluidlogged(oldBlockState, !newFluidState.isEmpty());
		} else { // other blocks => vanilla behaviour
			return oldBlockState;
		}
	}

	/**
	 * tries setting blockstate and fluidstate for pos. adjusts blockstate if needed (waterlogged, fluidblock level).
	 * Displaces fluid if needed. If block does not accept fluid and fluid can not be displaced, delete fluid.
	 *
	 * @param world
	 * @param pos
	 * @param blockState
	 * @param fluidState
	 * @return
	 */
	public static boolean setBlockAndFluid(World world, BlockPos pos, BlockState blockState, FluidState fluidState, boolean displaceFluid) {
		return setBlockAndFluid(world, pos, blockState, fluidState, displaceFluid, 3);
	}

	/**
	 * tries setting blockstate and fluidstate for pos. adjusts blockstate if needed (waterlogged, fluidblock level).
	 * Displaces fluid if needed. If block does not accept fluid and fluid can not be displaced, delete fluid.
	 *
	 * @param world
	 * @param pos
	 * @param blockState
	 * @param fluidState
	 * @param flags
	 * @return
	 */
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
		} else if (!displaceFluid) { // can not hold fluid and dont displace => destroy fluid
			boolean setBlockSuccess = ((WorldMixinInterface) world).setBlockNoFluid(pos, blockState, flags);
			boolean setFluidSuccess = ((WorldMixinInterface) world).setFluid(pos, Fluids.EMPTY.defaultFluidState(), flags);
			return setBlockSuccess && setFluidSuccess;
		} else if (recursion > 0) { // can not hold fluid => try displacing (prevent infinite recursion)
			FluidDisplacer displacer = new FluidDisplacer(world, fluidState);
			// flags = 3 is okay (block and client update + rerender + neighbor changes)
			return displacer.tryExecute(pos, 3, recursion - 1); // prevent infinite recursion
		} else { // recursion limit reached: error message with stack trace (but no exception)
			WPO.LOGGER.error("FFluidStatic.setBlockAndFluid and FluidDisplacer: reached recursion limit!", new Throwable());
			return false;
		}
	}

	public static void scheduleFluidTick(World world, BlockPos pos, FluidState fluidState) {
		if (!fluidState.isEmpty()) { // only schedule tick if contains fluid
			FlowingFluid flowingFluid = (FlowingFluid) fluidState.getType(); // safe cast, bc if not empty must be flowing
			world.getLiquidTicks().scheduleTick(pos, flowingFluid, flowingFluid.getTickDelay(world));
		}
	}

	/**
	 * returns whether state contains fluid (which requires special fluid handling) or is empty
	 * @param state
	 * @return
	 */
	public static boolean containsFluid(BlockState state){ // TODO level, pos -> FluidState
		if (isAirBlock(state)){
			return false; // by definition does not contain fluid
		} else if (isFluidBlock(state)){
			return true; // by definition contains fluid
		} else if (isFluidloggableBlock(state)){ // fluidloggable block can contain fluid or be empty
			return !state.getFluidState().isEmpty(); // enforced by getFluidState Mixins
		} else { // no fluid
			return false;
		}
	}

	/**
	 * removes fluid from any blockstate (can hold fluid OR not) and returns it
	 * @param state
	 * @return
	 */
	public static BlockState removeFluid(BlockState state){  // TODO level, pos, FluidState?
		if (isFluidBlock(state)){
			state = setFluid(state, Fluids.EMPTY);
			state = setFluidLevel(state, 0);
			return state;
		} else if (isFluidloggableBlock(state)){
			state = setFluid(state, Fluids.EMPTY);
			state = setFluidLevel(state, 0);
			state = setFluidlogged(state, false);
			return state;
		} else {
			return state;
		}
	}

	public static void ejectFluidOrDestroy(World world, BlockPos pos) {
		boolean success = FFluidStatic.ejectFluid(world, pos);
		if (!success) { // ejecting failed => destroy fluid
			((WorldMixinInterface) world).setFluidAndUpdate(pos, Fluids.EMPTY.defaultFluidState());
		}
	}

	/**
	 * eject fluid contained in fluidstate at pos
	 * @param world
	 * @param pos
	 * @return whether ejecting succeeded or not
	 */
	public static boolean ejectFluid(World world, BlockPos pos) {
		FluidState oldFluidState = world.getFluidState(pos);
		if (oldFluidState.isEmpty()) { // if empty
			return true; // succeeded at ejecting fluid :)
		} else { // if contains fluid
			// displace fluid contained in oldFluidState
			FluidDisplacer displacer = new FluidDisplacer(world, oldFluidState);
			return displacer.tryExecute(pos); // return success or not
		}
	}

	/**
	 * updates newState given oldState fluid:
	 * if newState cant hold fluid, ejects, otherwise copies fluid from oldState to newState<br/>
	 * (creates new fluid block when copying to air)
	 * @param world
	 * @param pos
	 * @param oldState
	 * @param newState
	 * @return updated newState
	 */
	public static BlockState copyFluidOrEject(ServerWorld world, BlockPos pos, BlockState oldState, BlockState newState){ // TODO level, pos, FluidState?, setFluid?
		if (!containsFluid(oldState)) { // if no fluid in oldState
			return newState; // nothing to copy TODO: maybe we need to remove fluid
		} else if (isAirBlock(newState)){ // if new block is air, create liquid block from old fluid
			FlowingFluid flowingFluid = (FlowingFluid) getFluid(oldState); // safe cast, because oldState contains fluid
			int fluidLevel = getFluidLevel(oldState);
			FluidState fluidState = getSourceOrFlowing(flowingFluid, fluidLevel);
			return fluidState.createLegacyBlock();
		} else if (canHoldFluid(newState)){ // if can hold => apply
			newState = setFluid(newState, getFluid(oldState));
			newState = setFluidLevel(newState, getFluidLevel(oldState));
			newState = setFluidlogged(newState, isFluidlogged(oldState));
			return newState;
		} else { // if newState can not hold => eject
			return ejectFluid(world, pos, oldState, newState);
		}
	}

	public static BlockState copyFluidOrEject(ServerWorld world, BlockPos pos, BlockState newState){ // TODO level, pos, FluidState?, setFluid?
		BlockState oldState = world.getBlockState(pos);
		return copyFluidOrEject(world, pos, oldState, newState);
	}

	/**
	 * Applies newlevel and fluid to state (waterlogs new block or creates water block).<br/>
	 * Does not check whether state can hold fluid!
	 * <br/><br/>
	 * Safe alternatives:
	 * - {@link FFluidStatic#copyFluidOrEject(ServerWorld, BlockPos, BlockState, BlockState)}
	 * Assumes that
	 * @param newState
	 * @param newLevel
	 * @param newFlowingFluid
	 * @return
	 */
	public static BlockState forceApplyFluid(BlockState newState, int newLevel, FlowingFluid newFlowingFluid) { // TODO level, pos, FluidState?
		// from FlowingFluid.spreadTo(IWorld, BlockPos, BlockState, ~, FluidState) => force places fluid and destroys if needed
//			if (pBlockState.getBlock() instanceof ILiquidContainer) { // if waterloggable
//				((ILiquidContainer)pBlockState.getBlock()).placeLiquid(pLevel, pPos, pBlockState, pFluidState); // place waterlogged
//			} else {
//				if (!pBlockState.isAir()) {
//					this.beforeDestroyingBlock(pLevel, pPos, pBlockState); // if not air, destroy (water/forge fluid => drop, lava => fizz)
//				}
//				pLevel.setBlock(pPos, pFluidState.createLegacyBlock(), 3);
//			}

		if (newLevel == 0) { // if no fluid (FlowingFluid is never empty)
			return removeFluid(newState); // TODO: remove just for safety (remove when all action iterators fixed)
		} else if (isAirBlock(newState)){ // if new block is air, create liquid block
			FluidState fluidState = getSourceOrFlowing(newFlowingFluid, newLevel);
			return fluidState.createLegacyBlock();
		} else if (canHoldFluid(newState)){ // if can hold (fluid block or fluidlogged) => apply
			newState = setFluid(newState, newFlowingFluid);
			newState = setFluidLevel(newState, newLevel);
			if (isFluidloggableBlock(newState)){ // if fluidloggable => set fluidlogged
				newState = setFluidlogged(newState, true);
			}
			return newState;
		} else {
			WPO.LOGGER.error("FFluidStatic.forceApplyFluid: Tried to force-apply fluid=" +
					newFlowingFluid.toString() + " level=" + newLevel +
					" to BlockState=" + newState.toString() +
					" which can not hold fluid and is not air!");
			return newState;
		}
	}

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
		if (state2.canOcclude() && !canHoldFluid(state2)) { // TODO need canOcclude?
			return false;
		}
		VoxelShape voxelShape1 = state1.getCollisionShape(world, fromPos);
		VoxelShape voxelShape2 = state2.getCollisionShape(world, fromPos.relative(inDirection));
		if (voxelShape1.isEmpty() && voxelShape2.isEmpty()) {
			return true;
		}
		return !VoxelShapes.mergedFaceOccludes(voxelShape1, voxelShape2, inDirection);
	}

	public static boolean canFlow(IBlockReader w, BlockPos fromPos, BlockPos toPos, BlockState state1, BlockState state2, Fluid fluid) {

		Fluid f2 = state2.getFluidState().getType();
		if (f2.isSame(fluid) && state1.getBlock() instanceof FlowingFluidBlock
				&& state2.getBlock() instanceof FlowingFluidBlock) {
			return true;
		}

		FluidPars fp2 = (FluidPars) ((IBlockExtended) state2.getBlock()).getCustomBlockPars().get(FluidPars.class);
		FluidPars fp1 = (FluidPars) ((IBlockExtended) state1.getBlock()).getCustomBlockPars().get(FluidPars.class);
		boolean posos = false;
		if (fp1 != null) {
			if (fp1.isPassable == 1) {
				posos = true;
			}
		}
		Direction dir = DirectionStatic.dirFromVec(fromPos, toPos);
		if (fp2 != null) {
			if (fp2.isPassable == 1) {
				// System.out.println(state2);
				return true;
			} else if (fp2.isPassable == -1) {
				return false;
			}
			if ((state2.getFluidState().isEmpty() || state1.getFluidState().canBeReplacedWith(w, fromPos, f2, dir))
					&& fp2.isDestroyableBy(fluid))
				return true;
		}

		if (state2.canOcclude() && !posos && !state2.hasProperty(WATERLOGGED)) {
			return false;
		}
		if (!(fluid instanceof WaterFluid) && (state1.hasProperty(WATERLOGGED) || state2.hasProperty(WATERLOGGED))) {
			return false;
		}
		VoxelShape voxelShape1 = state1.getCollisionShape(w, fromPos);
		VoxelShape voxelShape2 = state2.getCollisionShape(w, toPos);
		if ((voxelShape1.isEmpty() || posos) && voxelShape2.isEmpty()) {
			return true;
		}
		return !VoxelShapes.mergedFaceOccludes(voxelShape1, voxelShape2, dir);
	}

	// ================= MIXINS ==================//

	public static Vector3d getVel(IBlockReader w, BlockPos pos, FluidState fs) {

		Vector3d vel = new Vector3d(0, 0, 0);
		int level = fs.getAmount();
		BlockState state = fs.createLegacyBlock();
		Fluid fluid = fs.getType();
		BlockPos posu = pos.above();

		boolean flag = false;

		BlockState stateu = w.getBlockState(posu);

		if (canFlow(w, pos, posu, state, stateu, fluid) && !stateu.getFluidState().isEmpty()) {
			level += stateu.getFluidState().getAmount();
			flag = true;
		}

		for (Direction dir : Direction.Plane.HORIZONTAL) {
			BlockPos pos2 = pos.relative(dir);

			BlockState state2 = w.getBlockState(pos2);
			FluidState fs2 = state2.getFluidState();

			if (!fs2.isEmpty() && canFlow(w, pos, pos2, state, state2, fluid)) {
				int lvl2 = fs2.getAmount();
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
	 * only used by mixin into flowing fluid tick()
	 * @param world
	 * @param pos
	 * @param fluidState
	 */
	public static void tickFlowingFluid(World world, BlockPos pos, FluidState fluidState) {
		if (!fluidState.isEmpty()) { // if empty, skip ticking
			// TODO lava-water interaction: check LavaFluid.spreadTo() -- config: #lava packets == 1 stone (maybe consume 1 or #lava water packets?)
			// TODO make water sounds when packets move (ClientWorld.setFluid/setBlock)
//			if (pRandom.nextInt(64) == 0) {
//				pLevel.playLocalSound((double)pPos.getX() + 0.5D, (double)pPos.getY() + 0.5D, (double)pPos.getZ() + 0.5D, SoundEvents.WATER_AMBIENT, SoundCategory.BLOCKS, pRandom.nextFloat() * 0.25F + 0.75F, pRandom.nextFloat() + 0.5F, false);
//			}
			if (!world.isClientSide) { // only on server new fluid task
				FluidTasksManager.addFluidTask((ServerWorld) world, pos);
			}
		}
	}

	/**
	 * ONLY used for mixin. to schedule tick use Fluid.getTickDelay (already mixed in with this)
	 * @param oldRate
	 * @return
	 */
	public static int getTickDelay(int oldRate) {
		// vanilla => water: 5, lava: 30 (10 in nether)
		return (int) (oldRate * WPOConfig.COMMON.fluidTickRateScaling.get());
	}
}