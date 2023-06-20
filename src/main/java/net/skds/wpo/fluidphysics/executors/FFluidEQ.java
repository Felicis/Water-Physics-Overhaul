//package net.skds.wpo.fluidphysics.executors;
//
//import static net.skds.wpo.WPOConfig.COMMON;
//import static net.skds.wpo.util.Constants.MAX_FLUID_LEVEL;
//
//import net.minecraft.block.BlockState;
//import net.minecraft.block.IWaterLoggable;
//import net.minecraft.fluid.FluidState;
//import net.minecraft.fluid.WaterFluid;
//import net.minecraft.util.Direction;
//import net.minecraft.util.math.BlockPos;
//import net.minecraft.util.math.MathHelper;
//import net.minecraft.world.server.ServerWorld;
//import net.skds.wpo.WPOConfig;
//import net.skds.wpo.fluidphysics.FFluidStatic;
//import net.skds.wpo.fluidphysics.WorldWorkSet;
//import net.skds.wpo.util.Constants;
//
//public class FFluidEQ extends FFluidBasic {
//
//	public FFluidEQ(ServerWorld w, BlockPos pos, WorldWorkSet owner, FFluidBasic.Mode mode, int worker) {
//		super(w, pos, mode, owner, worker);
//	}
//
//	@Override
//	public void execute() {
//		// if above empty and can not flow down => do horizontal EQ
//        if (getBlockState(pos.above()).getFluidState().isEmpty() && !false
//                && !canFlow(pos, pos.below(), state, getBlockState(pos.below()), true, false)) {
//            equalize();
//        }
//    }
//
//	public void equalize() {
//		boolean slidingActive = WPOConfig.COMMON.maxSlideDist.get() > 0;
//		// boolean slide = false;
//		// setState(pos.add(0, 16, 0), Blocks.STONE.getDefaultState());
//		boolean hasSlided = false;
//		int i0 = w.getRandom().nextInt(4);
//		// if sliding active && can not flow down && level == 1 => try sliding
//		if (slidingActive && !canReach(pos, pos.below(), state, getBlockState(pos.below())) && level == 1) {
//			hasSlided = slide();
//		}
//		int maxEqDist = COMMON.maxEqDist.get();
//		boolean equalizationActive = maxEqDist > 0;
//		if (!hasSlided && equalizationActive) {
//			// if (isPassedEq(pos)) {
//			// return;
//			// }
//			for (int index = 0; index < 4; ++index) {
//				if (level <= 0) { // equalization done
//					break;
//				}
//				if (cancel) {
//					return;
//				}
//				Direction dir = Direction.from2DDataValue((index + i0) % 4);
//				equalizeLine(dir, false, maxEqDist);
//			}
//		}
//	}
//
//	public boolean slide() {
//		// setState(pos.add(0, 16, 0), Blocks.STONE.getDefaultState());
//		// System.out.println("x");
//		int slideDist = WPOConfig.COMMON.maxSlideDist.get();
//		int lenmin = slideDist;
//
//		boolean selPosb = false;
//		BlockPos slideDestPos = pos;
//		BlockState selState = state;
//
//		boolean[] diag2 = { false, true };
//
//		/// System.out.println("len");
//		for (Direction dir : FFluidStatic.getRandomizedDirections(w.getRandom(), false)) {
//			for (boolean diag : diag2) {
//
//				boolean slideDestFound = false;
//				BlockPos selPos2 = pos;
//				int dist = 0;
//				int len = lenmin;
//				BlockPos neighborPos = pos;
//				BlockPos thisPos = this.pos;
//				boolean side = false;
//				BlockState thisState = state;
//				BlockState neighborState = state;
//				boolean thisPosCanFlowDown = false;
//
//				// System.out.println(len);
//				wh: while (len > 0) {
//					thisPos = neighborPos;
//					thisState = neighborState;
//					if (diag) {
//						if (side) {
//							dir = dir.getClockWise();
//							side = !side;
//						} else {
//							dir = dir.getCounterClockWise();
//							side = !side;
//						}
//					}
//					neighborPos = thisPos.relative(dir);
//					neighborState = getBlockState(neighborPos);
//					FluidState neighborFluidState = getFluidState(neighborPos);
//					// if can flow to neighbor && (neighbor same fluid 1 level || empty fluid) => else ABORT
//					if (canReach(thisPos, neighborPos, thisState, neighborState)
//							&& (neighborFluidState.isEmpty() || (neighborFluidState.getAmount() < 2 && neighborFluidState.getType().isSame(fluid)))) {
//						// if waterloggbable but not water =>  ABORT...... TODO remove, we're doing all fluids now!!
//						if ((thisState.getBlock() instanceof IWaterLoggable || neighborState.getBlock() instanceof IWaterLoggable)
//								&& !(fluid instanceof WaterFluid)) {
//							break wh;
//						}
//						boolean notFirstIteration = dist > 0; // prevent selecting thisPos as destination
//						// if
//						if (notFirstIteration && !slideDestFound && neighborFluidState.isEmpty()) {
//							slideDestFound = true;
//							selPos2 = thisPos;
//						}
//                        thisPosCanFlowDown = canFlow(thisPos, thisPos.below(), thisState, getBlockState(thisPos.below()), true, false);
//					} else {
//						break wh;
//					}
//					--len;
//					// if flowing down, reset slideDist
//					if (thisPosCanFlowDown && slideDestFound) {
//						lenmin = Math.min(dist, lenmin);
//						slideDestPos = selPos2;
//						selState = thisState;
//						selPosb = true;
//					}
//					++dist;
//				}
//			}
//		}
//		if (selPosb && validate(slideDestPos)) {
//			//System.out.println("bl");
//			selState = getBlockState(slideDestPos);
//			selState = flowToPosEq(pos, slideDestPos, selState, -1);
//			setState(slideDestPos, selState);
//			setState(pos, state);
//			return true;
//		}
//		return false;
//	}
//
//	public void equalizeLine(Direction dir, boolean diag, int len) {
//		// len = (int) ((float) len * fluidWorker.eqSpeed);
//		// System.out.println(fluidWorker.eqSpeed);
//		// len=8;
//		BlockPos neighborPos = pos;
//		BlockPos thisPos = pos;
//		int len2 = len;
//		boolean side = false;
//		BlockState thisState = state;
//		BlockState neighborState = state;
//		int hmod = 0;
//		boolean bl = false;
//
//		boolean blocked = false;
//
//		while (len > 0) {
//
//			// if (diag) setState(pos1.down(), Blocks.BIRCH_LOG.getDefaultState());
//			// setState(pos1.add(0, 16, 0), Blocks.STONE.getDefaultState());
//
//			if (!diag && len2 - len == 1) {
//				equalizeLine(dir, true, len);
//			}
//
//			if (diag) {
//				if (side) {
//					dir = dir.getClockWise();
//					side = !side;
//				} else {
//					dir = dir.getCounterClockWise();
//					side = !side;
//				}
//			}
//			thisPos = neighborPos;
//			thisState = neighborState;
//
//			BlockPos aboveThisPos = thisPos.above();
//			BlockState aboveThisBS = getBlockState(aboveThisPos);
//			FluidState aboveThisFS = getFluidState(aboveThisPos);
//
//			if (!blocked && canReach(aboveThisPos, thisPos, aboveThisBS, thisState)
//					&& (!aboveThisFS.isEmpty() && isThisFluid(aboveThisFS.getType()))) {
//				// state1 = state1u;
//				// System.out.println("x");
//				neighborPos = aboveThisPos;
//				neighborState = aboveThisBS;
//				++hmod;
//				bl = true;
//			} else {
//				neighborPos = thisPos.relative(dir);
//				neighborState = getBlockState(neighborPos);
//			}
//
//			FluidState neighborFS = neighborState.getFluidState();
//
//			if (isPassedEq(neighborPos)) {
//				// fluidWorker.addNTTask(pos2.toLong(), FFluidStatic.getTickRate((FlowingFluid)
//				// fluid, w));
//				// fluidWorker.addNTTask(pos.toLong(), FFluidStatic.getTickRate((FlowingFluid)
//				// fluid, w));
//				// FluidTasksManager.addNTTask(w, pos1, FFluidStatic.getTickRate((FlowingFluid)
//				// fluid, w));
//				// System.out.println(pos2);
//				break;
//			}
//
//			if (canReach(thisPos, neighborPos, thisState, neighborState)
//					&& (isThisFluid(neighborFS.getType()) || (neighborFS.isEmpty() && level > 1))) {
//				if ((thisState.getBlock() instanceof IWaterLoggable || neighborState.getBlock() instanceof IWaterLoggable)
//						&& !(fluid instanceof WaterFluid)) {
//					// System.out.println("dd");
//					break;
//				}
//				bl = true;
//				blocked = false;
//
//			} else {
//				// pos1 = pos2;
//				neighborPos = thisPos.below();
//				thisState = neighborState;
//				neighborState = getBlockState(neighborPos);
//				neighborFS = neighborState.getFluidState();
//				if (canReach(thisPos, neighborPos, thisState, neighborState)
//						&& (!neighborFS.isEmpty() && isThisFluid(neighborFS.getType()) || neighborFS.isEmpty())) {
//					--hmod;
//					bl = true;
//					blocked = true;
//
//				} else {
//					break;
//				}
//			}
//
//			if (bl && !cancel && validate(neighborPos)) {
//				int level2 = neighborFS.getAmount();
//					//boolean b = level2 == 8 && level == 1;
//					//if (b) {
//					//	System.out.println(hmod);
//					//}
//				//int hmod2 = hmod >= 1 ? 1 : hmod <= -1 ? -1 : 0;
//				int l1 = getAbsoluteLevel(pos.getY(), level);
//				int l2 = getAbsoluteLevel(neighborPos.getY(), level2);
//                if (MathHelper.abs(l1 - l2) > 1 && !false) {
//                    neighborState = flowToPosEq(pos, neighborPos, neighborState, hmod);
//                    setState(neighborPos, neighborState);
//                    setState(pos, state);
//// System.out.println(level + " ss: " + level2 + state2);
//                    addPassedEq(neighborPos);
//                    return;
//                }
//            }
//			--len;
//		}
//	}
//
//	private BlockState flowToPosEq(BlockPos pos1, BlockPos pos2, BlockState state2, int l) {
//
//		BlockState state2n = state2;
//
//		FluidState fs2 = state2.getFluidState();
//		int level2 = fs2.getAmount();
//		int delta = (level - level2) / 2;
//		// l = 0;
//		if (l != 0) {
//			if (l == -1) {
//				level2 += level;
//				if (level2 > MAX_FLUID_LEVEL) {
//					level = level2 - MAX_FLUID_LEVEL;
//					level2 = MAX_FLUID_LEVEL;
//				} else {
//					level = 0;
//				}
//			} else {
//				// System.out.println(l);
//				level += level2;
//				if (level > MAX_FLUID_LEVEL) {
//					level2 = level - MAX_FLUID_LEVEL;
//					level = MAX_FLUID_LEVEL;
//				} else {
//					level2 = 0;
//				}
//			}
//			state = getUpdatedState(state, level);
//			state2n = getUpdatedState(state2, level2);
//
//		} else if (MathHelper.abs(delta) >= 1) {
//
//			level -= delta;
//			level2 += delta;
//			// System.out.println("Delta " + level + " ss: " + level2);
//			state = getUpdatedState(state, level);
//			state2n = getUpdatedState(state2, level2);
//
//		} else if (level2 == 0) {
//			level2 = level;
//			level = 0;
//			state = getUpdatedState(state, level);
//			state2n = getUpdatedState(state2, level2);
//		}
//		return state2n;
//
//	}
//
//	private boolean canFlow(BlockPos pos1, BlockPos pos2, BlockState state1, BlockState state2, boolean down,
//			boolean ignoreLevels) {
//		if (state2 == null) {
//			cancel = true;
//			return false;
//		}
//        if ((false || false) && !down) {
//			return false;
//		}
//        if (false && state1.getFluidState().getAmount() < Constants.MAX_FLUID_LEVEL) {
//			return false;
//		}
//
//		if ((state1.getBlock() instanceof IWaterLoggable || state2.getBlock() instanceof IWaterLoggable)
//				&& !(fluid instanceof WaterFluid)) {
//			return false;
//		}
//
//		if (!canReach(pos1, pos2, state1, state2)) {
//			return false;
//		}
//
//		FluidState fs2 = state2.getFluidState();
//		// if ((!fs2.isEmpty() && !isThisFluid(fs2.getFluid())) &&
//		// !state1.getFluidState().canDisplace(w, pos2,
//		// state2.getFluidState().getFluid(), FFluidStatic.dirFromVec(pos1, pos2)))
//		// return false;
//
//		int level2 = fs2.getAmount();
//		if (level2 >= MAX_FLUID_LEVEL && !ignoreLevels) {
//			return false;
//		}
//
//		if (level == 1 && !down && !ignoreLevels) {
//			if (fs2.isEmpty()) {
//				pos1 = pos2;
//				pos2 = pos2.below();
//				state1 = state2;
//				state2 = getBlockState(pos2);
//				if (isThisFluid(state2.getFluidState().getType()) || state2.getFluidState().isEmpty()) {
//					return canFlow(pos1, pos2, state1, state2, true, false);
//				} else {
//					return false;
//				}
//			} else {
//				return (level2 + 2 < level);
//			}
//		}
//
//		return true;
//	}
//}