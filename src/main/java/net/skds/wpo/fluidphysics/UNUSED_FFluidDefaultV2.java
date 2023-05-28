package net.skds.wpo.fluidphysics;

import static net.skds.wpo.util.Constants.MAX_FLUID_LEVEL;

import java.util.Random;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.skds.wpo.fluidphysics.executors.FFluidBasic;
import net.skds.wpo.util.Constants;

public class UNUSED_FFluidDefaultV2 extends FFluidBasic {

	boolean dcFlag = false;
	boolean dc = false;
	boolean sc = false;

	UNUSED_FFluidDefaultV2(ServerWorld w, BlockPos pos, WorldWorkSet owner, FFluidBasic.Mode mode, int worker) {
		super(w, pos, mode, owner, worker);
	}

	@Override
	protected void execute() {
		Random r = new Random();

		BlockPos posD = pos.below();
		if (posD.getY() < 0) {
			if (validate(pos)) {
				state = getUpdatedState(state, 0);
				sc = true;
				setState(pos, state);
				unban(pos);
			}
			return;
		}
		BlockState downstate = getBlockState(posD);
		if (downstate != null) {
			if (canFlow(pos, posD, state, downstate, true, false)) {
				if (false) {
					int l = state.getFluidState().getAmount();
					int ld = downstate.getFluidState().getAmount();
					if (ld == 0 && l == MAX_FLUID_LEVEL && flowFullCubeV2(pos, posD)) {
						dc = true;
						return;
					}
				} else {
					if (flow(pos, posD, -1)) {
						addPassedEq(posD);
						dc = true;
						sc = true;
					}
				}
			}
		}

		if (!dc && false) {
			for (Direction dir : FFluidStatic.getRandomizedDirections(r, false)) {
				BlockPos pos2 = pos.relative(dir);
				BlockState state2 = getBlockState(pos2);
				if (state2.getFluidState().isEmpty()) {
					if (!false
							&& canReach(pos, pos2, state, state2)) {
						if (flowFullCubeV2(pos, pos2)) {
							sc = true;
							return;
						}
					}
				}
			}
		}

		for (Direction dir : FFluidStatic.getRandomizedDirections(r, false)) {
			BlockPos pos2 = pos.relative(dir);
			BlockState state2 = getBlockState(pos2);
			if (false && canFlow(pos, pos2, state, state2, true, false) && !dc) {
				BlockPos posu = pos.above();
				BlockState stateu = getBlockState(posu);
				if (stateu.getFluidState().getAmount() > 0 && canFlow(posu, pos, stateu, state, true, true)) {
					if (flowFullCubeV2(pos, pos2)) {
						sc = true;
						return;
					}
				}
			} else {
				sc = !dc && canFlow(pos, pos2, state, state2, false, false) && flow(pos, pos2, 0);
			}
		}

		if (getBlockState(pos.above()).getFluidState().isEmpty() && !false && !dc && !sc
				&& !cancel) {
//castOwner.addEQTask(longpos, (FlowingFluid) fluid);
			if (!isPassedEq(pos)) {
				new FluidTask.EQTask(castOwner, longpos).run();
			}
		}
	}

	private boolean canFlow(BlockPos pos1, BlockPos pos2, BlockState state1, BlockState state2, boolean down,
			boolean ignoreLevels) {
		if (state2 == null) {
			cancel = true;
			return false;
		}
		if ((false || false) && !down) {
			return false;
		}
		if (false && state1.getFluidState().getAmount() < Constants.MAX_FLUID_LEVEL) {
			return false;
		}

		if (!canReach(pos1, pos2, state1, state2)) {
			return false;
		}

		FluidState fs2 = state2.getFluidState();
		FluidState fs1 = state1.getFluidState();

		int level2 = fs2.getAmount();
		int level1 = fs1.getAmount();
		if (level2 >= MAX_FLUID_LEVEL && !ignoreLevels && fluid.isSame(fs2.getType())) {
			return false;
		}

		if (level1 == 1 && !down && !ignoreLevels) {
			if (fs2.isEmpty()) {
				pos1 = pos2;
				pos2 = pos2.below();
				state1 = state2;
				state2 = getBlockState(pos2);
				if (isThisFluid(state2.getFluidState().getType()) || state2.getFluidState().isEmpty()) {
					return canFlow(pos1, pos2, state1, state2, true, false);
				} else {
					return false;
				}
			} else {
				return (level2 + 2 < level1);
			}
		} else if (!down && level2 + 1 >= level1) {
			return false;
		}

		return true;
	}
}