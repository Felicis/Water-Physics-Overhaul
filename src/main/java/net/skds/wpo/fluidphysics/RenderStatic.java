package net.skds.wpo.fluidphysics;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
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
        FluidState stated = null;

        FluidState state = w.getFluidState(pos);
        float level = state.getOwnHeight();
        float[] sum = new float[] { level, level, level, level };

        BlockPos posu = pos.above();
        BlockState statu = w.getBlockState(posu);
        FluidState ufs = w.getFluidState(posu);

        boolean posus = FFluidStatic.canFlowAndHold(w, pos, Direction.UP);

        if (fluid.isSame(ufs.getType()) && posus) {
            return new float[] { 1.0F, 1.0F, 1.0F, 1.0F };
        }

        posd = pos.below();
        stated = w.getFluidState(posd);
        downsuc = (stated.getType().isSame(fluid));

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
            FluidState state2 = w.getFluidState(pos2);

            boolean reach2 = FFluidStatic.canFlowAndHold(w, pos, dir);
            boolean same2 = state2.getType().isSame(fluid);
            if (same2 && reach2) {

                BlockPos pos2u = pos2.above();
                FluidState state2u = w.getFluidState(pos2u);
                if (state2u.getType().isSame(fluid)
                        && FFluidStatic.canFlowAndHold(w, pos2, Direction.UP)) {
                    conner[n] = true;
                    conner[n2] = true;
                    setconner[n] = true;
                    setconner[n2] = true;
                    setconnervl[n] = offset2;
                    setconnervl[n2] = offset2;
                } else {
                    float level2 = state2.getOwnHeight();
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
                    FluidState state2dir = w.getFluidState(pos2dir);
                    if (FFluidStatic.canFlowAndHold(w, pos2, dirside[i])) {

                        if (state2dir.getType().isSame(fluid)) {

                            BlockPos pos2diru = pos2dir.above();
                            FluidState state2diru = w.getFluidState(pos2diru);
                            if (state2diru.getType().isSame(fluid)
                                    && FFluidStatic.canFlowAndHold(w, pos2dir, Direction.UP)) {
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
                                float level2dir = state2dir.getOwnHeight();
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

                        } else if (state2dir.isEmpty()) {
                            BlockPos pos2dird = pos2dir.below();
                            FluidState state2dird = w.getFluidState(pos2dird);
                            if (state2dird.getType().isSame(fluid)
                                    && FFluidStatic.canFlowAndHold(w, pos2dir, Direction.DOWN)) {
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
                    FluidState state2d = w.getFluidState(pos2d);
                    if (state2d.getType().isSame(fluid)
                            && FFluidStatic.canFlowAndHold(w, pos2, Direction.DOWN)) {
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

    public static BlockRayTraceResult rayTrace(World worldIn, PlayerEntity player,
                                               RayTraceContext.FluidMode fluidMode) {
        float f = player.xRot;
        float f1 = player.yRot;
        Vector3d vector3d = player.getEyePosition(1.0F);
        float f2 = MathHelper.cos(-f1 * ((float) Math.PI / 180F) - (float) Math.PI);
        float f3 = MathHelper.sin(-f1 * ((float) Math.PI / 180F) - (float) Math.PI);
        float f4 = -MathHelper.cos(-f * ((float) Math.PI / 180F));
        float f5 = MathHelper.sin(-f * ((float) Math.PI / 180F));
        float f6 = f3 * f4;
        float f7 = f2 * f4;
        double d0 = player.getAttribute(net.minecraftforge.common.ForgeMod.REACH_DISTANCE.get()).getValue();
        Vector3d vector3d1 = vector3d.add((double) f6 * d0, (double) f5 * d0, (double) f7 * d0);
        return worldIn.clip(
                new RayTraceContext(vector3d, vector3d1, RayTraceContext.BlockMode.OUTLINE, fluidMode, player));
    }
}
