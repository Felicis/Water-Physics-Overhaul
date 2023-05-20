package net.skds.wpo.fluidphysics;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.PushReaction;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.*;
import net.minecraft.item.*;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.PistonEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.skds.core.api.IBlockExtended;
import net.skds.core.api.IWWSG;
import net.skds.core.api.IWorldExtended;
import net.skds.wpo.WPOConfig;
import net.skds.wpo.util.ExtendedFHIS;
import net.skds.wpo.util.pars.FluidPars;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

import static net.minecraft.state.properties.BlockStateProperties.WATERLOGGED;
import static net.skds.wpo.registry.BlockStateProps.FFLUID_LEVEL;

public class FFluidStatic {

	public final static int FCONST = 1000 / WPOConfig.MAX_FLUID_LEVEL;

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

	/**
	 * Applies newlevel and fluid to state: waterlogs or creates water block
	 *
	 * if block can not be waterlogged WILL DESTROY BLOCK!
	 * @param state0
	 * @param newlevel
	 * @param fluid
	 * @return
	 */
	public static BlockState getUpdatedState(BlockState state0, int newlevel, Fluid fluid) {
		if ((newlevel < 0) || (newlevel > WPOConfig.MAX_FLUID_LEVEL)) {
			throw new RuntimeException("Incorrect fluid level!!!");
		}
		if (state0.isAir() || state0.getBlock() instanceof FlowingFluidBlock){ // WATER, LAVA, Forge_Fluid
			// if air/fluid => create new fluid in place
			// TODO: check different fluid? check replace fluid?
			FluidState fsWithoutBlock;
			if (newlevel >= WPOConfig.MAX_FLUID_LEVEL) {
				fsWithoutBlock = ((FlowingFluid) fluid).getSource(false);
			} else if (newlevel <= 0) {
				fsWithoutBlock = Fluids.EMPTY.defaultFluidState();
			} else {
				fsWithoutBlock = ((FlowingFluid) fluid).getFlowing(newlevel, false);
			}
			return fsWithoutBlock.createLegacyBlock();
		} else { // real block (not air)
			if (state0.hasProperty(WATERLOGGED)) {
				boolean hasWater = newlevel >= 1;
				if (state0.hasProperty(FFLUID_LEVEL)) {
					return state0.setValue(WATERLOGGED, hasWater).setValue(FFLUID_LEVEL, newlevel);
				} else {
					return state0.setValue(WATERLOGGED, hasWater); // TODO: this duplicates water
				}
			} else { // not waterloggable
				return state0; // TODO destroys water?
			}
		}
	}

	public static float getHeight(int level) {
		float h = ((float) level / WPOConfig.MAX_FLUID_LEVEL) * 0.9375F;
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

	public static PushReaction getPushReaction(BlockState state) {
		return PushReaction.PUSH_ONLY;
	}

	public static boolean isSameFluid(Fluid f1, Fluid f2) {
		if (f1 == Fluids.EMPTY)
			return false;
		if (f2 == Fluids.EMPTY)
			return false;
		return f1.isSame(f2);
	}

	public static int getTickRate(FlowingFluid fluid, IWorldReader w) {
		int rate = fluid.getTickDelay(w);
		rate /= 2;
		//System.out.println(rate);
		return rate > 0 ? rate : 1;
	}

	public static Direction dirFromVec(BlockPos pos, BlockPos pos2) {
		return Direction.getNearest(pos2.getX() - pos.getX(), pos2.getY() - pos.getY(),
				pos2.getZ() - pos.getZ());
	}

	// ================ OTHER ================== //

	public static Vector3d getVel2(IBlockReader w, BlockPos posV, FluidState state) {

		Vector3d vel = new Vector3d(0, 0, 0);
		int level = state.getAmount();
		Iterator<Direction> iter = Direction.Plane.HORIZONTAL.iterator();

		while (iter.hasNext()) {
			Direction dir = (Direction) iter.next();
			BlockPos pos2 = posV.relative(dir);

			BlockState st = w.getBlockState(pos2);
			FluidState fluidState = st.getFluidState();
			if (!fluidState.isEmpty() && canReach(w, posV, dir.getOpposite())) {
				int lvl0 = fluidState.getAmount();
				FluidState f2 = w.getFluidState(pos2.above());
				if (isSameFluid(state.getType(), f2.getType())) {
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

	// ================ RENDERER ================== //

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

		boolean posus = canReach(pos, posu, state, statu, fluid, w);

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

			boolean reach2 = canReach(pos, pos2, state, state2, fluid, w);
			boolean same2 = state2.getFluidState().getType().isSame(fluid);
			if (same2 && reach2) {

				BlockPos pos2u = pos2.above();
				BlockState state2u = w.getBlockState(pos2u);
				if (state2u.getFluidState().getType().isSame(fluid)
						&& canReach(pos2, pos2u, state2, state2u, fluid, w)) {
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
					if (canReach(pos2, pos2dir, state2, state2dir, fluid, w)) {

						if (state2dir.getFluidState().getType().isSame(fluid)) {

							BlockPos pos2diru = pos2dir.above();
							BlockState state2diru = w.getBlockState(pos2diru);
							if (state2diru.getFluidState().getType().isSame(fluid)
									&& canReach(pos2dir, pos2diru, state2dir, state2diru, fluid, w)) {
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
									&& canReach(pos2dir, pos2dird, state2dir, state2dird, fluid, w)) {
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
							&& canReach(pos2, pos2d, state2, state2d, fluid, w)) {
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

	public static float getConH(IBlockReader w, BlockPos p, Fluid f, BlockPos dir) {
		// p = p.add(-dir.getX(), 0, -dir.getZ());
		// Blockreader w = (Blockreader) wi;
		BlockPos pu = p.above();
		FluidState ufs = w.getFluidState(pu);
		if (!ufs.isEmpty() && isSameFluid(ufs.getType(), f)) {
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

		if (!dfs.isEmpty() && isSameFluid(dfs.getType(), f)) {
			pu = dp.above();
			ufs = w.getFluidState(pu);
			if (!ufs.isEmpty() && isSameFluid(ufs.getType(), f)) {
				return 1.0f;
			}

			sl += dfs.getOwnHeight();
			i++;
			s = true;
		} else if (dfs.isEmpty() && canReach(w, p, Direction.getNearest(dir.getX(), 0, 0))) {
			BlockPos downp = dp.below();
			FluidState downfs = w.getFluidState(downp);
			if (!downfs.isEmpty() && isSameFluid(downfs.getType(), f) && downfs.getOwnHeight() == 1.0F) {
				return 0.0F;
			}
		}

		if (!dfs2.isEmpty() && isSameFluid(dfs2.getType(), f)) {
			pu = dp2.above();
			ufs = w.getFluidState(pu);
			if (!ufs.isEmpty() && isSameFluid(ufs.getType(), f)) {
				return 1.0f;
			}

			sl += dfs2.getOwnHeight();
			i++;
			s = true;
		} else if (dfs2.isEmpty() && canReach(w, p, Direction.getNearest(0, 0, dir.getZ()))) {
			BlockPos downp = dp2.below();
			FluidState downfs = w.getFluidState(downp);
			if (!downfs.isEmpty() && isSameFluid(downfs.getType(), f) && downfs.getOwnHeight() == 1.0F) {
				return 0.0F;
			}
		}

		if (s) {
			BlockPos dp3 = p.offset(dir);
			FluidState dfs3 = w.getFluidState(dp3);

			if (!dfs3.isEmpty() && isSameFluid(dfs3.getType(), f)) {
				pu = dp3.above();
				ufs = w.getFluidState(pu);
				if (!ufs.isEmpty() && isSameFluid(ufs.getType(), f)) {
					return 1.0f;
				}

				sl += dfs3.getOwnHeight();
				i++;
			} else if (dfs3.isEmpty()) {
				BlockPos downp = dp3.below();
				FluidState downfs = w.getFluidState(downp);
				if (!downfs.isEmpty() && isSameFluid(downfs.getType(), f) && downfs.getOwnHeight() == 1.0F
						&& canReach(w, dp3, Direction.getNearest(0, 1, 0))) {
					return 0.0F;
				}
			}
		}
		return sl /= i;
	}

	// ================= UTIL ================== //
	private static boolean canReach(IBlockReader world, BlockPos pos, Direction direction) {
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
		Direction dir = dirFromVec(pos1, pos2);
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

	public static boolean canOnlyFullCube(BlockState bs){
		return bs.hasProperty(WATERLOGGED) && ! bs.hasProperty(FFLUID_LEVEL);
	}

	public static BlockState getStateWithFluid(BlockState state, ItemUseContext context) {
		return getStateWithFluid(state, context.getLevel(), context.getClickedPos());
	}

	public static BlockState getStateWithFluid(BlockState state, World world, BlockPos pos) {
		BlockState oldBlockState = world.getBlockState(pos);
		if (state != null && oldBlockState != null) {
			return getStateWithFluid(state, oldBlockState);
		}
		return state;
	}

	public static BlockState getStateWithFluid(BlockState blockState, BlockState oldBlockState) {
		if (oldBlockState.hasProperty(WATERLOGGED) && blockState.hasProperty(WATERLOGGED)) {
			Fluid oldFluid = oldBlockState.getFluidState().getType();
			if (oldBlockState.hasProperty(FFLUID_LEVEL) && blockState.hasProperty(FFLUID_LEVEL)){
				return getUpdatedState(blockState, oldBlockState.getValue(FFLUID_LEVEL), oldFluid);
			} else {
				return blockState.setValue(WATERLOGGED, oldBlockState.getValue(WATERLOGGED));
			}
		}
		return blockState;
	}

	// ================= ITEMS ==================//

	public static void onBucketEvent(FillBucketEvent e) {

		FishBucketItem fishItem = null;
		ItemStack bucket = e.getEmptyBucket();
		Item bu = bucket.getItem();
		if (bu instanceof FishBucketItem) {
			fishItem = (FishBucketItem) bu;
		}
		Optional<IFluidHandlerItem> op = bucket.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY)
				.resolve();
		IFluidHandlerItem bh;
		if (op.isPresent()) {
			bh = op.get();
		} else {
			bh = new ExtendedFHIS(bucket, 1000);
			// System.out.println("l;hhhhhhh " + bh);
		}
		Fluid bucketFluid = bh.getFluidInTank(0).getFluid();
		if (!(bucketFluid instanceof FlowingFluid) && bucketFluid != Fluids.EMPTY) {
			return;
		}
		PlayerEntity p = e.getPlayer();
		World w = e.getWorld();
		BlockRayTraceResult targ = rayTrace(w, p,
				bucketFluid == Fluids.EMPTY ? RayTraceContext.FluidMode.ANY : RayTraceContext.FluidMode.NONE);
		if (targ.getType() != RayTraceResult.Type.BLOCK) {
			return;
		}
		BlockPos pos = targ.getBlockPos();
		BlockState bs = w.getBlockState(pos);
		FluidState fs = bs.getFluidState();
		Fluid blockFluid = fs.getType();
		// if target block not interactable OR item not usable on target block
		if (!(w.mayInteract(p, pos) && p.mayUseItemAt(pos, targ.getDirection(), bh.getContainer()))) {
			return;
		}

		if (bucketFluid == Fluids.EMPTY) { // PICK UP
			if (blockFluid == Fluids.EMPTY) {
				return; // nothing to pick up
			}
			// pick up
			BucketFiller filler = new BucketFiller(w, blockFluid, bh, e);
			iterateFluidWay(WPOConfig.COMMON.maxBucketDist.get(), pos, filler); // why not equalize greater area?
		} else { // PLACE
			// if block cannot accept water => try next block towards player (e.g. place water against wall)
			if (fs.isEmpty() && !bs.hasProperty(WATERLOGGED)) {
				pos = pos.relative(targ.getDirection());
				bs = w.getBlockState(pos);
			}
			if (!w.isClientSide && bs.hasProperty(WATERLOGGED)) {
				FluidTasksManager.addFluidTask((ServerWorld) w, pos, bs);
			}
			if (!bucketFluid.isSame(blockFluid) && blockFluid != Fluids.EMPTY) {
				e.setCanceled(true); // cannot place fluid into a different fluid
				return;
			}
			// place fluid
			BucketFlusher flusher = new BucketFlusher(w, bucketFluid, bh, e);
			if (iterateFluidWay(WPOConfig.COMMON.maxBucketDist.get(), pos, flusher) && fishItem != null) {
				fishItem.checkExtraContent(w, bucket, pos);
			}
		}
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

	private static class BucketFiller implements IFluidActionIteratable {

		int bucketLevels = WPOConfig.MAX_FLUID_LEVEL;
		int sl = 0;
		boolean complete = false;
		World world;
		Fluid fluid;
		FillBucketEvent event;
		IFluidHandlerItem bucket;
		Long2ObjectLinkedOpenHashMap<BlockState> states = new Long2ObjectLinkedOpenHashMap<>();

		BucketFiller(World w, Fluid f, IFluidHandlerItem b, FillBucketEvent e) {
			world = w;
			fluid = f;
			bucket = b;
			event = e;
		}

		@Override
		public boolean isComplete() {
			return complete;
		}

		@Override
		public void run(BlockPos pos, BlockState state) {
			// world.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5,
			// pos.getZ() + 0.5, 0, 0, 0);

			if (canOnlyFullCube(state) && state.getValue(WATERLOGGED)) {
				states.clear();
				states.put(pos.asLong(), getUpdatedState(state, 0, fluid));
				complete = true;
				return;
			}
			FluidState fs = state.getFluidState();
			int l = fs.getAmount();
			sl += l;
			int nl = 0;
			if (sl >= bucketLevels) {
				nl = sl - bucketLevels;
				complete = true;
			}
			states.put(pos.asLong(), getUpdatedState(state, nl, fluid));
		}

		@Override
		public World getWorld() {
			return world;
		}

		@Override
		public boolean isValidState(BlockState state) {
			return fluid.isSame(state.getFluidState().getType());
		}

		@Override
		public void fail() {
			// System.out.println(sl);

			fillStates(states, world);
			event.setResult(Result.ALLOW);
			PlayerEntity p = event.getPlayer();
			Item item = bucket.getContainer().getItem();
			p.awardStat(Stats.ITEM_USED.get(item));
			SoundEvent soundevent = fluid.getAttributes().getFillSound();
			if (soundevent == null)
				soundevent = fluid.is(FluidTags.LAVA) ? SoundEvents.BUCKET_FILL_LAVA
						: SoundEvents.BUCKET_FILL;
			p.playSound(soundevent, 1.0F, 1.0F);

			if (!p.abilities.instabuild) {
				ItemStack stack = new ItemStack(net.skds.wpo.registry.Items.ADVANCED_BUCKET.get());
				ExtendedFHIS st2 = new ExtendedFHIS(stack, 1000);
				Fluid f2 = fluid instanceof FlowingFluid ? ((FlowingFluid) fluid).getSource() : fluid;
				FluidStack fluidStack = new FluidStack(f2, sl * FFluidStatic.FCONST);
				st2.fill(fluidStack, FluidAction.EXECUTE);

				stack = st2.getContainer();
				event.setFilledBucket(stack);
			}
		}

		@Override
		public void finish() {
			fillStates(states, world);

			event.setResult(Result.ALLOW);
			PlayerEntity p = event.getPlayer();
			Item item = bucket.getContainer().getItem();
			p.awardStat(Stats.ITEM_USED.get(item));
			SoundEvent soundevent = fluid.getAttributes().getFillSound();
			if (soundevent == null)
				soundevent = fluid.is(FluidTags.LAVA) ? SoundEvents.BUCKET_FILL_LAVA
						: SoundEvents.BUCKET_FILL;
			p.playSound(soundevent, 1.0F, 1.0F);
			if (!p.abilities.instabuild) {
				// bucket.fill(new FluidStack(fluid, 1000), FluidAction.EXECUTE);
				event.setFilledBucket(new ItemStack(fluid.getBucket()));
			}
		}
	}

	private static class BottleFiller implements IFluidActionIteratable {

		int bucketLevels = 3;
		int sl = 0;
		boolean complete = false;
		World world;
		ItemStack bottle;
		Fluid fluid;
		CallbackInfoReturnable<ActionResult<ItemStack>> ci;
		Long2ObjectLinkedOpenHashMap<BlockState> states = new Long2ObjectLinkedOpenHashMap<>();

		BottleFiller(World w, Fluid f, CallbackInfoReturnable<ActionResult<ItemStack>> ci, ItemStack stack) {
			world = w;
			fluid = f;
			bottle = stack;
			this.ci = ci;
		}

		@Override
		public boolean isComplete() {
			return complete;
		}

		@Override
		public void run(BlockPos pos, BlockState state) {
			// world.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5,
			// pos.getZ() + 0.5, 0, 0, 0);

			if (canOnlyFullCube(state) && state.getValue(WATERLOGGED)) {
				states.clear();
				states.put(pos.asLong(), getUpdatedState(state, 0, fluid));
				complete = true;
				return;
			}
			FluidState fs = state.getFluidState();
			int l = fs.getAmount();
			int osl = sl;
			sl += l;
			int nl = 0;
			if (sl >= bucketLevels) {
				nl = sl - bucketLevels;
				complete = true;
			}
			if (osl != sl)
				states.put(pos.asLong(), getUpdatedState(state, nl, fluid));
		}

		@Override
		public World getWorld() {
			return world;
		}

		@Override
		public boolean isValidState(BlockState state) {
			return fluid.isSame(state.getFluidState().getType());
		}

		@Override
		public void finish() {
			fillStates(states, world);
		}

		@Override
		public void fail() {
			ci.setReturnValue(ActionResult.fail(bottle));
		}
	}

	private static class BucketFlusher implements IFluidActionIteratable {

		int maxStateLvl = WPOConfig.MAX_FLUID_LEVEL;
		int bucketLvl;
		boolean complete = false;
		World world;
		Fluid bucketFluid;
		FillBucketEvent event;
		IFluidHandlerItem bucket;
		Long2ObjectLinkedOpenHashMap<BlockState> states = new Long2ObjectLinkedOpenHashMap<>();

		BucketFlusher(World w, Fluid f, IFluidHandlerItem b, FillBucketEvent e) {
			world = w;
			bucket = b;
			event = e;
			bucketLvl = bucket.getFluidInTank(0).getAmount() / FFluidStatic.FCONST;
			bucketFluid = bucket.getFluidInTank(0).getFluid();
		}

		@Override
		public boolean isComplete() {
			return complete;
		}

		@Override
		public void run(BlockPos pos, BlockState state) {
			// world.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5,
			// pos.getZ() + 0.5, 0, 0, 0);

			// only full block & not waterlogged yet
			if (canOnlyFullCube(state) &&  !state.getValue(WATERLOGGED)) {
				states.clear();
				states.put(pos.asLong(), getUpdatedState(state, maxStateLvl, bucketFluid)); // duplicate liquid!!!!
				complete = true;
				return;
			}

			FluidState fs = state.getFluidState();
			int stateLvl = fs.getAmount();
			int freeBlockLvls = maxStateLvl - stateLvl;
			if (freeBlockLvls <= 0){
				// block already full
				return;
			} else if (freeBlockLvls < bucketLvl){
				// empty bucket partially
				int newStateLvl = maxStateLvl;
				bucketLvl -= freeBlockLvls;
				states.put(pos.asLong(), getUpdatedState(state, newStateLvl, bucketFluid));
			} else if (freeBlockLvls >= bucketLvl) {
				// empty bucket fully
				int newStateLvl = stateLvl + bucketLvl;
				bucketLvl = 0;
				states.put(pos.asLong(), getUpdatedState(state, newStateLvl, bucketFluid));
			}

			if (bucketLvl == 0){
				complete = true;
				return;
			}
		}

		@Override
		public World getWorld() {
			return world;
		}

		@Override
		public boolean isValidState(BlockState state) {
			return bucketFluid.isSame(state.getFluidState().getType()) || state.getFluidState().isEmpty();
		}

		@Override
		public void finish() {
			fillStates(states, world);

			event.setResult(Result.ALLOW);
			PlayerEntity p = event.getPlayer();
			Item item = bucket.getContainer().getItem();
			p.awardStat(Stats.ITEM_USED.get(item));
			SoundEvent soundevent = bucketFluid.getAttributes().getEmptySound();
			if (soundevent == null)
				soundevent = bucketFluid.is(FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA
						: SoundEvents.BUCKET_EMPTY;
			p.playSound(soundevent, 1.0F, 1.0F);
			if (!p.abilities.instabuild) {
				// bucket.fill(FluidStack.EMPTY, FluidAction.EXECUTE);
				// event.setFilledBucket(bucket.getContainer());
				event.setFilledBucket(new ItemStack(Items.BUCKET));
			}
		}
	}

	public static class FluidDisplacer implements IFluidActionIteratable {

		int mfl = WPOConfig.MAX_FLUID_LEVEL;
		// int bucketLevels = PhysEXConfig.MAX_FLUID_LEVEL;
		int sl;
		boolean complete = false;
		World world;
		Fluid fluid;
		BlockEvent.EntityPlaceEvent event;
		Long2ObjectLinkedOpenHashMap<BlockState> states = new Long2ObjectLinkedOpenHashMap<>();
		BlockState obs;

		FluidDisplacer(World w, BlockEvent.EntityPlaceEvent e) {
			obs = e.getBlockSnapshot().getReplacedBlock();
			FluidState ofs = obs.getFluidState();

			fluid = ofs.getType();
			sl = ofs.getAmount();
			world = w;
			event = e;
		}

		@Override
		public boolean isComplete() {
			return complete;
		}

		@Override
		public void addZero(Set<BlockPos> set, BlockPos p0) {
			for (Direction d : getRandomizedDirections(world.getRandom(), true)) {
				BlockPos pos2 = p0.relative(d);
				BlockState state2 = world.getBlockState(pos2);
				if (isValidState(state2) && canReach(p0, pos2, obs, state2, fluid, world)) {
					set.add(pos2);
				}
			}
		}

		@Override
		public void run(BlockPos pos, BlockState state) {
			// world.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5,
			// pos.getZ() + 0.5, 0, 0, 0);

			// if (fb) {
			// fb = false;
			// return;
			// }

			if (canOnlyFullCube(state) && state.hasProperty(WATERLOGGED) && !state.getValue(WATERLOGGED)) {
				states.clear();
				states.put(pos.asLong(), getUpdatedState(state, mfl, fluid));
				complete = true;
				return;
			}
			FluidState fs = state.getFluidState();
			int el = mfl - fs.getAmount();
			int osl = sl;
			sl -= el;
			int nl = mfl;
			if (sl <= 0) {
				nl = mfl + sl;
				complete = true;
			}
			if (osl != sl)
				states.put(pos.asLong(), getUpdatedState(state, nl, fluid));
		}

		@Override
		public World getWorld() {
			return world;
		}

		@Override
		public boolean isValidState(BlockState state) {
			return fluid.isSame(state.getFluidState().getType()) || state.getFluidState().isEmpty();
		}

		@Override
		public void finish() {
			fillStates(states, world);
			event.setResult(Result.ALLOW);
		}

		@Override
		public void fail() {
			// event.setCanceled(true);
		}
	}

	public static class FluidDisplacer2 implements IFluidActionIteratable {

		int mfl = WPOConfig.MAX_FLUID_LEVEL;
		// int bucketLevels = PhysEXConfig.MAX_FLUID_LEVEL;
		int sl;
		boolean complete = false;
		World world;
		Fluid fluid;
		Long2ObjectLinkedOpenHashMap<BlockState> states = new Long2ObjectLinkedOpenHashMap<>();
		BlockState obs;

		FluidDisplacer2(World w, BlockState obs) {
			FluidState ofs = obs.getFluidState();
			this.obs = obs;
			fluid = ofs.getType();
			sl = ofs.getAmount();
			world = w;
		}

		@Override
		public boolean isComplete() {
			return complete;
		}

		@Override
		public void addZero(Set<BlockPos> set, BlockPos p0) {
			for (Direction d : getRandomizedDirections(world.getRandom(), true)) {
				BlockPos pos2 = p0.relative(d);
				BlockState state2 = world.getBlockState(pos2);
				if (isValidState(state2) && canReach(p0, pos2, obs, state2, fluid, world)) {
					set.add(pos2);
				}
			}
		}

		@Override
		public void run(BlockPos pos, BlockState state) {
			// world.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5,
			// pos.getZ() + 0.5, 0, 0, 0);

			// if (fb) {
			// fb = false;
			// return;
			// }

			if (canOnlyFullCube(state) && state.hasProperty(WATERLOGGED) && !state.getValue(WATERLOGGED)) {
				states.clear();
				states.put(pos.asLong(), getUpdatedState(state, mfl, fluid));
				complete = true;
				return;
			}
			FluidState fs = state.getFluidState();
			int el = mfl - fs.getAmount();
			int osl = sl;
			sl -= el;
			int nl = mfl;
			if (sl <= 0) {
				nl = mfl + sl;
				complete = true;
			}
			if (osl != sl)
				states.put(pos.asLong(), getUpdatedState(state, nl, fluid));
		}

		@Override
		public World getWorld() {
			return world;
		}

		@Override
		public boolean isValidState(BlockState state) {
			return fluid.isSame(state.getFluidState().getType()) || state.getFluidState().isEmpty();
		}

		@Override
		public void finish() {
			fillStates(states, world);
		}

		@Override
		public void fail() {
			// event.setCanceled(true);
		}
	}

	private static class PistonDisplacer implements IFluidActionIteratable {

		int mfl = WPOConfig.MAX_FLUID_LEVEL;
		// int bucketLevels = PhysEXConfig.MAX_FLUID_LEVEL;
		int remainingLvl;
		boolean complete = false;
		World world;
		Fluid fluid;
		// PistonBlockStructureHelper ps;
		Set<BlockPos> movepos = new HashSet<>();
		PistonEvent.Pre event;
		Long2ObjectLinkedOpenHashMap<BlockState> states = new Long2ObjectLinkedOpenHashMap<>();
		BlockState oldBS;

		PistonDisplacer(World w, PistonEvent.Pre e, BlockState os, PistonBlockStructureHelper ps) {
			this.oldBS = os;
			FluidState oldFS = oldBS.getFluidState();
			// this.ps = ps;
			this.fluid = oldFS.getType();
			this.remainingLvl = oldFS.getAmount();
			this.world = w;
			this.event = e;
			movepos.addAll(ps.getToDestroy());
			movepos.addAll(ps.getToPush());
			for (BlockPos p : ps.getToPush()) {
				movepos.add(p.relative(event.getDirection()));
				// System.out.println(p.relative(event.getDirection()));
			}
		}

		@Override
		public boolean isComplete() {
			return complete;
		}

		@Override
		public void addZero(Set<BlockPos> set, BlockPos p0) {
			for (Direction d : getRandomizedDirections(world.getRandom(), true)) {
				BlockPos pos2 = p0.relative(d);
				BlockState state2 = world.getBlockState(pos2);
				if (isValidState(state2) && canReach(p0, pos2, oldBS, state2, fluid, world)) {
					set.add(pos2);
				}
			}
		}

		@Override
		public void run(BlockPos pos, BlockState state) {
			// world.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5,
			// pos.getZ() + 0.5, 0, 0, 0);

			// if (fb) {
			// fb = false;
			// return;
			// }

			if (canOnlyFullCube(state) && state.hasProperty(WATERLOGGED) && !state.getValue(WATERLOGGED)) {
				states.clear();
				states.put(pos.asLong(), getUpdatedState(state, mfl, fluid));
				complete = true;
				return;
			}
			FluidState fs = state.getFluidState();
			int freeStateLvl = mfl - fs.getAmount();
			remainingLvl -= freeStateLvl;
			int newLvl;
			if (remainingLvl <= 0) {
				newLvl = mfl + remainingLvl;
				complete = true;
			} else {
				newLvl = mfl;
			}
			if (freeStateLvl != 0) // something has been added
				states.put(pos.asLong(), getUpdatedState(state, newLvl, fluid));
		}

		@Override
		public World getWorld() {
			return world;
		}

		@Override
		public boolean isValidState(BlockState state) {
			return fluid.isSame(state.getFluidState().getType()) || state.getFluidState().isEmpty();
		}

		@Override
		public boolean isValidPos(BlockPos pos) {
			return !movepos.contains(pos);
		}

		@Override
		public void finish() {
			fillStates(states, world);
			event.setResult(Result.ALLOW);
			// System.out.println("u");
		}

		@Override
		public void fail() {
			event.setCanceled(true);
			// event.setResult(Result.DENY);
			// System.out.println("x");
		}
	}

	public static void fillStates(Long2ObjectLinkedOpenHashMap<BlockState> states, World world) {
		if (!world.isClientSide) {
			states.forEach((lpos, state) -> {
				world.setBlockAndUpdate(BlockPos.of(lpos), state);
			});
		}
	}

	public interface IFluidActionIteratable {
		default void addZero(Set<BlockPos> set, BlockPos p0) {
			set.add(p0);
		}

		boolean isComplete();

		void run(BlockPos pos, BlockState state);

		World getWorld();

		boolean isValidState(BlockState state);

		default boolean isValidPos(BlockPos pos) {
			return true;
		}

		void finish();

		default void fail() {
		}
	}

	public static void onBottleUse(World w, PlayerEntity p, Hand hand,
			CallbackInfoReturnable<ActionResult<ItemStack>> ci, ItemStack stack) {
		BlockRayTraceResult rt = rayTrace(w, p, RayTraceContext.FluidMode.ANY);
		BlockPos pos = rt.getBlockPos();

		BottleFiller filler = new BottleFiller(w, Fluids.WATER, ci, stack);
		iterateFluidWay(WPOConfig.COMMON.maxBucketDist.get(), pos, filler);
	}

	public static void onBlockPlace(BlockEvent.EntityPlaceEvent e) {
		World w = (World) e.getWorld();
		BlockPos pos = e.getPos();
		BlockState oldState = e.getBlockSnapshot().getReplacedBlock();
		FluidState oldFluidState = oldState.getFluidState();
		Fluid oldFluid = oldFluidState.getType();
		BlockState newState = e.getPlacedBlock();
		Block newBlock = newState.getBlock();
		if (oldFluidState.isEmpty() || newBlock instanceof SpongeBlock || newBlock instanceof WetSpongeBlock) {
			return;
		}
		// frost walker replaces water with water (idk why) => delete water (since it is created again from melting ice)
		// idk when FrostedIceBlock is placed...
		int frostWalkerLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.FROST_WALKER, (LivingEntity) e.getEntity());
		if (frostWalkerLevel > 0 && newBlock == Blocks.WATER && newState.getMaterial() == Material.WATER){
			return; // does not create water since frost walker does not trigger on partially filled water blocks
		}
		if (newBlock instanceof ILiquidContainer && !newState.hasProperty(WATERLOGGED)) {
			return; // probably only kelp and seagrass (dont exist w/o water) => do not push water
		}
		//		return bs.getBlock() instanceof IWaterLoggable;
		if (newState.hasProperty(WATERLOGGED)){
			if (newState.hasProperty(FFLUID_LEVEL)){
				// custom fluid level
				newState = getUpdatedState(newState, oldFluidState.getAmount(), oldFluid);
				w.setBlockAndUpdate(pos, newState);
				return;
			} else {
				// vanilla: only waterlogged TODO: does this work as intended?
				return; // minecraft already sets WATERLOGGED (on place)
			}
		}

		// else push water out
		FluidDisplacer displacer = new FluidDisplacer(w, e);
		iterateFluidWay(10, e.getPos(), displacer);
	}

	// ======================= PISTONS ======================= //

	public static void onPistonPre(PistonEvent.Pre e) {
		World w = (World) e.getWorld();
		if (w.isClientSide || e.isCanceled()) {
			return;
		}
		PistonBlockStructureHelper ps = e.getStructureHelper();

		if (!ps.resolve()) {
			return;
		}
		List<BlockPos> poslist = ps.getToDestroy();

		for (BlockPos pos : poslist) {
			BlockState state = w.getBlockState(pos);
			FluidState fs = state.getFluidState();
			// System.out.println(state);

			if (!fs.isEmpty()) {

				// System.out.println("jjj");

				PistonDisplacer displacer = new PistonDisplacer(w, e, state, ps);
				if (!iterateFluidWay(12, pos, displacer)) {
					e.setCanceled(true);
				}
			}
		}
	}
}