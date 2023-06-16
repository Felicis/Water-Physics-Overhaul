package net.skds.wpo.fluidphysics;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.world.IBlockReader;

import java.util.Iterator;

public class UnusedStatic {
    public static Vector3d getVel2(IBlockReader w, BlockPos posV, FluidState state) {

        Vector3d vel = new Vector3d(0, 0, 0);
        int level = state.getAmount();
        Iterator<Direction> iter = Direction.Plane.HORIZONTAL.iterator();

        while (iter.hasNext()) {
            Direction dir = (Direction) iter.next();
            BlockPos pos2 = posV.relative(dir);

            BlockState st = w.getBlockState(pos2);
            FluidState fluidState = st.getFluidState();
            if (!fluidState.isEmpty() && FFluidStatic.canFlow(w, posV, dir.getOpposite())) {
                int lvl0 = fluidState.getAmount();
                FluidState f2 = w.getFluidState(pos2.above());
                if (FFluidStatic.isSameFluid(state.getType(), f2.getType())) {
                    lvl0 += f2.getAmount();
                }
                int delta = level - lvl0;
                if (delta > 1 || delta < -1) {
                    Vector3i v3i = dir.getNormal();
                    vel = vel.add(v3i.getX() * delta, 0, v3i.getZ() * delta);
                }
            }
            // vel.multiply((double) 1D/n);
        }
        return vel.normalize();
    }

    public static float getConH(IBlockReader w, BlockPos p, Fluid f, BlockPos dir) {
        // p = p.add(-dir.getX(), 0, -dir.getZ());
        // Blockreader w = (Blockreader) wi;
        BlockPos pu = p.above();
        FluidState ufs = w.getFluidState(pu);
        if (!ufs.isEmpty() && FFluidStatic.isSameFluid(ufs.getType(), f)) {
            return 1.0f;
        }
        FluidState fsm = w.getFluidState(p);

        float sl = fsm.getOwnHeight();
        int i = 1;
        BlockPos dp = p.offset(dir.getX(), 0, 0);
        BlockPos dp2 = p.offset(0, 0, dir.getZ());
        FluidState dfs = w.getFluidState(dp);
        FluidState dfs2 = w.getFluidState(dp2);

        boolean s = false;

        if (!dfs.isEmpty() && FFluidStatic.isSameFluid(dfs.getType(), f)) {
            pu = dp.above();
            ufs = w.getFluidState(pu);
            if (!ufs.isEmpty() && FFluidStatic.isSameFluid(ufs.getType(), f)) {
                return 1.0f;
            }

            sl += dfs.getOwnHeight();
            i++;
            s = true;
        } else if (dfs.isEmpty() && FFluidStatic.canFlow(w, p, Direction.getNearest(dir.getX(), 0, 0))) {
            BlockPos downp = dp.below();
            FluidState downfs = w.getFluidState(downp);
            if (!downfs.isEmpty() && FFluidStatic.isSameFluid(downfs.getType(), f) && downfs.getOwnHeight() == 1.0F) {
                return 0.0F;
            }
        }

        if (!dfs2.isEmpty() && FFluidStatic.isSameFluid(dfs2.getType(), f)) {
            pu = dp2.above();
            ufs = w.getFluidState(pu);
            if (!ufs.isEmpty() && FFluidStatic.isSameFluid(ufs.getType(), f)) {
                return 1.0f;
            }

            sl += dfs2.getOwnHeight();
            i++;
            s = true;
        } else if (dfs2.isEmpty() && FFluidStatic.canFlow(w, p, Direction.getNearest(0, 0, dir.getZ()))) {
            BlockPos downp = dp2.below();
            FluidState downfs = w.getFluidState(downp);
            if (!downfs.isEmpty() && FFluidStatic.isSameFluid(downfs.getType(), f) && downfs.getOwnHeight() == 1.0F) {
                return 0.0F;
            }
        }

        if (s) {
            BlockPos dp3 = p.offset(dir);
            FluidState dfs3 = w.getFluidState(dp3);

            if (!dfs3.isEmpty() && FFluidStatic.isSameFluid(dfs3.getType(), f)) {
                pu = dp3.above();
                ufs = w.getFluidState(pu);
                if (!ufs.isEmpty() && FFluidStatic.isSameFluid(ufs.getType(), f)) {
                    return 1.0f;
                }

                sl += dfs3.getOwnHeight();
                i++;
            } else if (dfs3.isEmpty()) {
                BlockPos downp = dp3.below();
                FluidState downfs = w.getFluidState(downp);
                if (!downfs.isEmpty() && FFluidStatic.isSameFluid(downfs.getType(), f) && downfs.getOwnHeight() == 1.0F
                        && FFluidStatic.canFlow(w, dp3, Direction.getNearest(0, 1, 0))) {
                    return 0.0F;
                }
            }
        }
        return sl /= i;
    }

//	public static PushReaction getPushReaction(BlockState state) {
//		return PushReaction.PUSH_ONLY;
//	}
}
