package net.skds.wpo.fluidphysics;

import net.minecraft.block.*;
import net.minecraft.fluid.*;
import net.minecraft.item.*;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.skds.core.api.IBlockExtended;
import net.skds.core.api.IWWSG;
import net.skds.core.api.IWorldExtended;
import net.skds.wpo.WPO;
import net.skds.wpo.WPOConfig;
import net.skds.wpo.fluidphysics.actioniterables.*;
import net.skds.wpo.util.Constants;
import net.skds.wpo.util.marker.WPOFluidMarker;
import net.skds.wpo.util.marker.WPOFluidloggableMarker;
import net.skds.wpo.util.pars.FluidPars;

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

	public static boolean isEmpty(Fluid fluid){
		// DO NOT CALL state.getFluidState() => recursion
		// replaces: protected Fluid.isEmpty()
		return fluid.isSame(Fluids.EMPTY);
	}

	public static boolean isFluidOrFluidloggable(BlockState state){
		return isFluidBlock(state) || isFluidloggableBlock(state);
	}

	/**
	 * returns true if state is a FluidBlock and is modded by WPO to have custom leveling<br/>
	 * use to check for pure fluid blocks (to be handled by WPO)
	 * @param state
	 * @return
	 */
	public static boolean isFluidBlock(BlockState state){
		Block block = state.getBlock();
		return WPOFluidMarker.isWPOFluid(block);
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
	 * checks whether block can contain fluid (with exceptions)
	 * <br/>
	 * This is the case when:<br/>
	 * a) the block is a (pure) fluid block<br/>
	 * b) the block is a "normal" block extended by WPO to be waterlogged with arbitrary fluids and levels<br/><br/>
	 * Air blocks can not directly contain fluid, but they can be replaced with Fluid blocks.
	 * @param state
	 * @return
	 */
	public static boolean canHoldFluid(BlockState state){
//		if (isAirBlock(state)) // a) block is air (does not have WATERLOGGED or LEVEL, but can receive)
//			return false; // has to be replaced with Fluid or Fluidloggable block first
		if (isFluidBlock(state)){ // b) WPO fluid block
			return true;
		} else if (isFluidloggableBlock(state)){ // c) WPO waterlogged block
			// accumulate exceptions here:
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
	public static boolean isFluidlogged(BlockState state){
		return state.getValue(WATERLOGGED);
	}

	/**
	 * setter for WATERLOGGED property (only access through this!)
	 * @param state
	 * @param fluidlogged
	 * @return
	 */
	public static BlockState setFluidlogged(BlockState state, boolean fluidlogged){
		return state.setValue(WATERLOGGED, fluidlogged);
	}

	/**
	 * getter for WPO_LEVEL property (only access through this!)
	 * @param state
	 * @return
	 */
	public static int getFluidLevel(BlockState state){
		return state.getValue(WPO_LEVEL);
	}

	/**
	 * setter for WPO_LEVEL property (only access through this!)
	 * @param state
	 * @param level
	 * @return
	 */
	public static BlockState setFluidLevel(BlockState state, int level){
		if (level < 0 || level > Constants.MAX_FLUID_LEVEL) {
			throw new RuntimeException("Incorrect fluid level!!!");
		} else {
			return state.setValue(WPO_LEVEL, level);
		}
	}

	/**
	 * getter for WPO_FLUID property (only access through this!)
	 * @param state
	 * @return
	 */
	public static Fluid getFluid(BlockState state){
		return property2Fluid(state.getValue(WPO_FLUID));
	}

	/**
	 * setter for WPO_FLUID (only access through this!)
	 * @param state
	 * @param fluid
	 * @return
	 */
	public static BlockState setFluid(BlockState state, Fluid fluid){
		return state.setValue(WPO_FLUID, fluid2Property(fluid));
	}

	/**
	 *
	 * @param state
	 * @return
	 */
	public static FluidState getFluidState(BlockState state){
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

	public static void tryScheduleFluidTick(IWorld world, BlockPos pos, BlockState state) {
		if (containsFluid(state)) { // only schedule tick if contains fluid
			FlowingFluid flowingFluid = (FlowingFluid) getFluid(state);
			int tickRate = flowingFluid.getTickDelay(world);
			world.getLiquidTicks().scheduleTick(pos, flowingFluid, tickRate);
		}
	}

	/**
	 * returns whether state contains fluid (which requires special fluid handling) or is empty
	 * @param state
	 * @return
	 */
	public static boolean containsFluid(BlockState state){
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
	public static BlockState removeFluid(BlockState state){
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

	/**
	 * ejects fluid contained in oldState and removes fluid from newState
	 * @param world
	 * @param pos
	 */
	public static BlockState ejectFluid(ServerWorld world, BlockPos pos, BlockState oldState, BlockState newState) {
		// TODO schedule for worker thread?
		// displace fluid contained in oldState
		FluidDisplacer displacer = new FluidDisplacer(world, oldState);
		iterateFluidWay(10, pos, displacer); // TODO magic number
		return removeFluid(newState); // removes fluid from newState
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
	public static BlockState copyFluidOrEject(ServerWorld world, BlockPos pos, BlockState oldState, BlockState newState){
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

	public static BlockState copyFluidOrEject(BlockState newState, ItemUseContext context) {
		World world = context.getLevel();
		if (!world.isClientSide){
			return copyFluidOrEject((ServerWorld) world, context.getClickedPos(), newState);
		} else {
			return newState;
		}
	}

	public static BlockState copyFluidOrEject(ServerWorld world, BlockPos pos, BlockState newState){
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
	public static BlockState forceApplyFluid(BlockState newState, int newLevel, FlowingFluid newFlowingFluid) {
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

	public static int getTickDelay(int oldRate) {
		// vanilla => water: 5, lava: 30 (10 in nether)
		return (int) (oldRate * WPOConfig.COMMON.fluidTickRateScaling.get());
	}

	// ================ OTHER ================== //

	public static Vector3d getVel(IBlockReader w, BlockPos pos, FluidState fs) {

		Vector3d vel = new Vector3d(0, 0, 0);
		int level = fs.getAmount();
		BlockState state = fs.createLegacyBlock();
		Fluid fluid = fs.getType();
		BlockPos posu = pos.above();

		boolean flag = false;

		BlockState stateu = w.getBlockState(posu);

		if (canReach(pos, posu, state, stateu, fluid, w) && !stateu.getFluidState().isEmpty()) {
			level += stateu.getFluidState().getAmount();
			flag = true;
		}

		for (Direction dir : Direction.Plane.HORIZONTAL) {
			BlockPos pos2 = pos.relative(dir);

			BlockState state2 = w.getBlockState(pos2);
			FluidState fs2 = state2.getFluidState();

			if (!fs2.isEmpty() && canReach(pos, pos2, state, state2, fluid, w)) {
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

	// ================= UTIL ================== //
	public static boolean canReach(IBlockReader world, BlockPos pos, Direction direction) {
		BlockState state1 = world.getBlockState(pos);
		BlockState state2 = world.getBlockState(pos.relative(direction));
		if (state2.canOcclude() && !state2.hasProperty(WATERLOGGED)) {
			return false;
		}
		VoxelShape voxelShape2 = state2.getCollisionShape(world, pos.relative(direction));
		VoxelShape voxelShape1 = state1.getCollisionShape(world, pos);
		if (voxelShape1.isEmpty() && voxelShape2.isEmpty()) {
			return true;
		}
		return !VoxelShapes.mergedFaceOccludes(voxelShape1, voxelShape2, direction);
	}

	public static boolean canReach(BlockPos pos1, BlockPos pos2, BlockState state1, BlockState state2, Fluid fluid,
								   IBlockReader w) {

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
		Direction dir = DirectionStatic.dirFromVec(pos1, pos2);
		if (fp2 != null) {
			if (fp2.isPassable == 1) {
				// System.out.println(state2);
				return true;
			} else if (fp2.isPassable == -1) {
				return false;
			}
			if ((state2.getFluidState().isEmpty() || state1.getFluidState().canBeReplacedWith(w, pos1, f2, dir))
					&& fp2.isDestroyableBy(fluid))
				return true;
		}

		if (state2.canOcclude() && !posos && !state2.hasProperty(WATERLOGGED)) {
			return false;
		}
		if (!(fluid instanceof WaterFluid) && (state1.hasProperty(WATERLOGGED) || state2.hasProperty(WATERLOGGED))) {
			return false;
		}
		VoxelShape voxelShape2 = state2.getCollisionShape(w, pos2);
		VoxelShape voxelShape1 = state1.getCollisionShape(w, pos1);
		if ((voxelShape1.isEmpty() || posos) && voxelShape2.isEmpty()) {
			return true;
		}
		return !VoxelShapes.mergedFaceOccludes(voxelShape1, voxelShape2, dir);
	}

	// ================= ITEMS ==================//

	public static boolean iterateFluidWay(int maxRange, BlockPos pos, IFluidActionIteratable actioner) {
		boolean first = true;
		boolean client;
		World w = actioner.getWorld();
		IWWSG wws = ((IWorldExtended) w).getWWS();
		Set<BlockPos> setBan = new HashSet<>();
		Set<BlockPos> setAll = new HashSet<>();
		client = (wws == null);

		if (!client && setBan.add(pos) && !wws.banPos(pos.asLong())) {
			setBan.forEach(p -> wws.banPos(p.asLong()));
			///wws.unbanPoses(setBan);
			return false;
		}
		setAll.add(pos);
		Set<BlockPos> setLocal = new HashSet<>();
		actioner.addZero(setLocal, pos);
		int n = maxRange;
		while (n > 0 && !actioner.isComplete() && !setLocal.isEmpty()) {
			--n;
			Set<BlockPos> setLocal2 = new HashSet<>();
			for (BlockPos posn : setLocal) {
				if (first) {
					first = false;
					setAll.add(posn);
					BlockState bs = w.getBlockState(posn);
					if (!client && setBan.add(posn) && !wws.banPos(posn.asLong())) {
						//wws.unbanPoses(setBan);
						setBan.forEach(p -> wws.unbanPos(p.asLong()));
						return false;
					}
					actioner.run(posn, bs);
					// w.addParticle(ParticleTypes.CLOUD, posn.getX() + 0.5, posn.getY() + 0.5,
					// posn.getZ() + 0.5, 0, 0, 0);
				}
				if (actioner.isComplete()) {
					break;
				}
				for (Direction dir : getRandomizedDirections(w.getRandom(), true)) {
					BlockPos pos2 = posn.relative(dir);
					if (setAll.contains(pos2)) {
						continue;
					}
					BlockState bs2 = w.getBlockState(pos2);
					boolean cr = canReach(w, posn, dir);
					boolean eq = actioner.isValidState(bs2);
					if (cr && eq) {
						setLocal2.add(pos2);
						if (actioner.isValidPos(pos2)) {
							if (!client && setBan.add(pos2) && !wws.banPos(pos2.asLong())) {
								//wws.unbanPoses(setBan);
								setBan.forEach(p -> wws.unbanPos(p.asLong()));
								return false;
							}
							actioner.run(pos2, bs2);
						}
						// w.addParticle(ParticleTypes.CLOUD, pos2.getX() + 0.5, pos2.getY() + 0.5,
						// pos2.getZ() + 0.5, 0, 0, 0);
					}
					// if (!eq) {

					setAll.add(pos2);
					// }
					if (actioner.isComplete()) {
						break;
					}
				}
			}
			setLocal = setLocal2;
		}
		// if (!client) {
		// for (BlockPos p : setAll) {
		// if (!wws.banPos(p)) {
		// wws.unbanPoses(setAll);
		// System.out.println(p);
		// return false;
		// }
		// }
		//
		// }
		if (actioner.isComplete()) {
			actioner.finish();
			if (!client)
				//wws.unbanPoses(setBan);
				setBan.forEach(p -> wws.unbanPos(p.asLong()));
			return true;
		} else {
			actioner.fail();
			if (!client)
				//wws.unbanPoses(setBan);
				setBan.forEach(p -> wws.unbanPos(p.asLong()));
			return false;
		}
	}

}