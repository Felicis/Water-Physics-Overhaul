package net.skds.wpo.mixin.unused;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.block.LeavesBlock;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.skds.wpo.registry.BlockStateProps;
import net.skds.wpo.util.interfaces.IBaseWL;

@Mixin(value = { DoorBlock.class, FenceGateBlock.class, LeavesBlock.class })
public class ExtraWLBlockMixin extends Block implements IBaseWL, IWaterLoggable {

	public ExtraWLBlockMixin(Properties properties) {
		super(properties);
	}

	public void customStatesRegister(Block b, StateContainer.Builder<Block, BlockState> builder) {

		builder.add(BlockStateProps.WPO_LEVEL);
		try {
			builder.add(BlockStateProperties.WATERLOGGED);
		} catch (Exception e) {
		}
	}

	@Inject(method = "<init>", at = @At(value = "TAIL"))
	protected void ccc(AbstractBlock.Properties properties, CallbackInfo ci) {
		if (this.defaultBlockState().hasProperty(BlockStateProperties.WATERLOGGED)) {
			// this.setDefaultState(this.getDefaultState().with(BlockStateProperties.WATERLOGGED,
			// Boolean.valueOf(false)));
			//DefaultStateFixer.addToFix(this);
		}
	}

	@Override
	public void fixDS() {
		this.registerDefaultState(this.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, Boolean.valueOf(false)));
	}
}