package net.skds.wpo.mixins.block;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.AbstractBlock.AbstractBlockState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.skds.wpo.WPOConfig;
import net.skds.wpo.fluidphysics.FFluidStatic;
import net.skds.wpo.registry.BlockStateProps;
import net.skds.wpo.util.interfaces.IBaseWL;

import static net.minecraft.state.properties.BlockStateProperties.WATERLOGGED;
import static net.skds.wpo.registry.BlockStateProps.FFLUID_LEVEL;

@Mixin(value = { AbstractBlockState.class })
public abstract class AbstractBlockStateMixin {

	@Inject(method = "getFluidState", at = @At(value = "HEAD"), cancellable = true)
	public void getFluidStateM(CallbackInfoReturnable<FluidState> ci) {
		BlockState bs = (BlockState) (Object) this;
		if (bs.hasProperty(WATERLOGGED) && bs.hasProperty(FFLUID_LEVEL)) {
			int level = bs.getValue(FFLUID_LEVEL);
			FluidState fs;
			if (bs.getValue(WATERLOGGED)) {
				level = (level == 0) ? WPOConfig.MAX_FLUID_LEVEL : level; // why this?
				if (level >= WPOConfig.MAX_FLUID_LEVEL) {
					fs = Fluids.WATER.getSource(false);
				} else if (level <= 0) {
					fs = Fluids.EMPTY.defaultFluidState();
				} else {
					fs = Fluids.WATER.getFlowing(level, false);
				}
			} else {
				fs = Fluids.EMPTY.defaultFluidState();
			}
			ci.setReturnValue(fs);
		}

	}

	@Inject(method = "isRandomlyTicking", at = @At(value = "HEAD"), cancellable = true)
	public void isRandomlyTickingM(CallbackInfoReturnable<Boolean> ci) {
	}

	@Inject(method = "neighborChanged", at = @At(value = "HEAD"), cancellable = false)
	public void neighborChangedM(World worldIn, BlockPos posIn, Block blockIn, BlockPos fromPosIn, boolean isMoving,
			CallbackInfo ci) {
		// super.neighborChanged(worldIn, posIn, blockIn, fromPosIn, isMoving);
		BlockState thisBS = (BlockState) (Object) this;
		if (thisBS.hasProperty(WATERLOGGED) && thisBS.hasProperty(FFLUID_LEVEL)) {
			fixFFLNoWL(worldIn, thisBS, posIn);
			if (thisBS.getValue(WATERLOGGED))
				worldIn.getLiquidTicks().scheduleTick(posIn, thisBS.getFluidState().getType(),
						FFluidStatic.getTickRate((FlowingFluid) thisBS.getFluidState().getType(), worldIn));
		}
	}

	@Inject(method = "updateShape", at = @At(value = "HEAD"), cancellable = false)
	public void updateShapeM(Direction face, BlockState queried, IWorld worldIn, BlockPos currentPos,
			BlockPos offsetPos, CallbackInfoReturnable<BlockState> ci) {
		BlockState thisBS = (BlockState) (Object) this;
		if (thisBS.hasProperty(WATERLOGGED) && thisBS.hasProperty(FFLUID_LEVEL)) {
			fixFFLNoWL(worldIn, thisBS, currentPos);
			if (thisBS.getValue(WATERLOGGED))
				worldIn.getLiquidTicks().scheduleTick(currentPos, thisBS.getFluidState().getType(),
						FFluidStatic.getTickRate((FlowingFluid) thisBS.getFluidState().getType(), worldIn));
		}
	}

	private void fixFFLNoWL(IWorld w, BlockState s, BlockPos p) {
		if (!s.getValue(WATERLOGGED) && s.getValue(FFLUID_LEVEL) > 0) {
			w.setBlock(p, s.setValue(FFLUID_LEVEL, 0), 3);
		}
	}
}