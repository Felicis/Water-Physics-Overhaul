package net.skds.wpo.fluidphysics;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.skds.wpo.util.Constants;

public class RenderStatic {
    public static float getHeight(int level) {
        float h = ((float) level / Constants.MAX_FLUID_LEVEL) * 0.9375F;
        switch (level) {
            case 3:
                return h * 0.9F;
            case 2:
                return h * 0.75F;
            case 1:
                return h * 0.4F;
            default:
                return h;
        }
    }

    public static float[] getConH(IBlockReader w, BlockPos pos, Fluid fluid) {
        int[] count = new int[] { 1, 1, 1, 1 };
        boolean[] conner = new boolean[4];
        boolean[] setconner = new boolean[4];
        float[] setconnervl = new float[4];
        // boolean downtry = false;
        boolean downsuc = false;

        float offset = 0.0036F;
        float offset2 = 0.99999F;

        BlockPos posd = null;
        BlockState stated = null;

        BlockState state = w.getBlockState(pos);
        float level = state.getFluidState().getOwnHeight();
        float[] sum = new float[] { level, level, level, level };

        BlockPos posu = pos.above();
        BlockState statu = w.getBlockState(posu);
        FluidState ufs = w.getFluidState(posu);

        boolean posus = FFluidStatic.canReach(pos, posu, state, statu, fluid, w);

        if (fluid.isSame(ufs.getType()) && posus) {
            return new float[] { 1.0F, 1.0F, 1.0F, 1.0F };
        }

        posd = pos.below();
        stated = w.getBlockState(posd);
        downsuc = (stated.getFluidState().getType().isSame(fluid));

        if (posus) {
            offset2 = 1.0F;
        }

        if (downsuc) {
            offset = 0.0F;
        }

        // int n = -1;
        Direction dir = Direction.EAST;
        for (int n = 0; n < 4; n++) {
            dir = dir.getCounterClockWise();
            // ++n;
            int n2 = n > 0 ? n - 1 : 3;
            BlockPos pos2 = pos.relative(dir);
            BlockState state2 = w.getBlockState(pos2);

            boolean reach2 = FFluidStatic.canReach(pos, pos2, state, state2, fluid, w);
            boolean same2 = state2.getFluidState().getType().isSame(fluid);
            if (same2 && reach2) {

                BlockPos pos2u = pos2.above();
                BlockState state2u = w.getBlockState(pos2u);
                if (state2u.getFluidState().getType().isSame(fluid)
                        && FFluidStatic.canReach(pos2, pos2u, state2, state2u, fluid, w)) {
                    conner[n] = true;
                    conner[n2] = true;
                    setconner[n] = true;
                    setconner[n2] = true;
                    setconnervl[n] = offset2;
                    setconnervl[n2] = offset2;
                } else {
                    float level2 = state2.getFluidState().getOwnHeight();
                    sum[n] += level2;
                    sum[n2] += level2;
                    count[n]++;
                    count[n2]++;
                }
                Direction[] dirside = new Direction[2];
                dirside[0] = dir.getClockWise();
                dirside[1] = dir.getCounterClockWise();

                for (int i = 0; i < 2; i++) {
                    if (i == 0 && (conner[n2])) {
                        continue;
                    }
                    if (i == 1 && (conner[n])) {
                        continue;
                    }
                    BlockPos pos2dir = pos2.relative(dirside[i]);
                    BlockState state2dir = w.getBlockState(pos2dir);
                    if (FFluidStatic.canReach(pos2, pos2dir, state2, state2dir, fluid, w)) {

                        if (state2dir.getFluidState().getType().isSame(fluid)) {

                            BlockPos pos2diru = pos2dir.above();
                            BlockState state2diru = w.getBlockState(pos2diru);
                            if (state2diru.getFluidState().getType().isSame(fluid)
                                    && FFluidStatic.canReach(pos2dir, pos2diru, state2dir, state2diru, fluid, w)) {
                                if (i == 0) {
                                    setconnervl[n2] = offset2;
                                    setconner[n2] = true;
                                    conner[n2] = true;
                                } else {
                                    setconnervl[n] = offset2;
                                    setconner[n] = true;
                                    conner[n] = true;
                                }
                            } else {
                                float level2dir = state2dir.getFluidState().getOwnHeight();
                                if (i == 0) {
                                    sum[n2] += level2dir;
                                    count[n2]++;
                                    conner[n2] = true;
                                } else {
                                    sum[n] += level2dir;
                                    count[n]++;
                                    conner[n] = true;
                                }
                            }

                        } else if (state2dir.getFluidState().isEmpty()) {
                            BlockPos pos2dird = pos2dir.below();
                            BlockState state2dird = w.getBlockState(pos2dird);
                            if (state2dird.getFluidState().getType().isSame(fluid)
                                    && FFluidStatic.canReach(pos2dir, pos2dird, state2dir, state2dird, fluid, w)) {
                                if (i == 0) {
                                    if (!setconner[n2])
                                        setconnervl[n2] = offset;
                                    setconner[n2] = true;
                                    conner[n2] = true;
                                } else {
                                    if (!setconner[n2])
                                        setconnervl[n] = offset;
                                    setconner[n] = true;
                                    conner[n] = true;
                                }
                            }
                        }
                    }
                }
            } else {

                if (reach2) {
                    BlockPos pos2d = pos2.below();
                    BlockState state2d = w.getBlockState(pos2d);
                    if (state2d.getFluidState().getType().isSame(fluid)
                            && FFluidStatic.canReach(pos2, pos2d, state2, state2d, fluid, w)) {
                        if (!setconner[n]) {
                            setconner[n] = true;
                            setconnervl[n] = offset;
                        }
                        if (!setconner[n2]) {
                            setconner[n2] = true;
                            setconnervl[n2] = offset;
                        }
                    }
                }
            }
        }

        float[] ch = new float[4];
        for (int i = 0; i < 4; i++) {
            if (setconner[i]) {
                ch[i] = setconnervl[i];
            } else {
                ch[i] = (float) sum[i] / count[i];
            }
        }
        return ch;
    }
}