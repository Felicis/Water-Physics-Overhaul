package net.skds.wpo.mixin.blockstate;

import net.minecraft.block.AbstractBlock.AbstractBlockState;
import net.minecraft.fluid.FluidState;
import net.skds.wpo.WPO;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = { AbstractBlockState.class })
public abstract class AbstractBlockStateMixin {

	/**
	 * RoadBlock Injection to catch FluidState creation
	 */
	@Inject(method = "getFluidState", at = @At(value = "HEAD"), cancellable = true)
	public void getFluidStateM(CallbackInfoReturnable<FluidState> cir) throws Exception {
		WPO.LOGGER.error("AbstractBlockState.getFluidState() was called!!!!", new Throwable());
//		throw new Exception("AbstractBlockState.getFluidState() was called!!!!");
//		cir.setReturnValue(null);
	}

	// TODO schedule fluid ticks when neighbors change/update shape/... (maybe do in FluidStateMixin?)
//	@Inject(method = "neighborChanged", at = @At(value = "HEAD"))
//	public void neighborChangedM(World world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving,
//								 CallbackInfo ci) {
//		BlockState state = (BlockState) (Object) this;
//		FFluidStatic.tryScheduleFluidTick(world, pos, state);
//	}

//	@Inject(method = "updateShape", at = @At(value = "HEAD"))
//	public void updateShapeM(Direction face, BlockState queried, IWorld worldIn, BlockPos currentPos,
//							 BlockPos offsetPos, CallbackInfoReturnable<BlockState> ci) {
//		BlockState state = (BlockState) (Object) this;
//		FFluidStatic.tryScheduleFluidTick(worldIn, currentPos, state);
//	}

//	private void fixFFLNoWL(IWorld w, BlockState s, BlockPos p) { // TODO remove if state stays stable; else use in mixins
//		if (!s.getValue(WATERLOGGED) && s.getValue(WPO_LEVEL) > 0) {
//			w.setBlock(p, s.setValue(WPO_LEVEL, 0), 3);
//		}
//	}
}