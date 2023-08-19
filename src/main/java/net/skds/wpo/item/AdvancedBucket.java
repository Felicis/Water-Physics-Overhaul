package net.skds.wpo.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import net.skds.wpo.fluidphysics.Constants;

import javax.annotation.Nullable;
import java.util.List;

public class AdvancedBucket extends BucketItem implements ICapabilityProvider {

	public AdvancedBucket(Fluid fluid, Properties builder) {
		super(() -> fluid, builder);
	}

	private FluidHandler fluidHandler;

	public static AdvancedBucket getBucketForReg(Fluid fluid) {
		Properties prop = new Properties().stacksTo(fluid == Fluids.EMPTY ? 16 : 1)
				.defaultDurability(Constants.MAX_FLUID_LEVEL).setNoRepair();
		return new AdvancedBucket(fluid, prop);
	}

	@Override
	public ActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn) {
		return super.use(worldIn, playerIn, handIn);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip,
			ITooltipFlag flagIn) {
		FluidHandler fh = new FluidHandler(stack);
		FluidStack fst = fh.getFluid();
		Fluid f = fst.getFluid();
		//Block b = f.getDefaultState().getBlockState().getBlock();
		TextFormatting form = TextFormatting.DARK_PURPLE;
		//ITextComponent texComp = new TranslationTextComponent(b.getTranslationKey()).mergeStyle(form);
		ITextComponent texComp = new TranslationTextComponent(f.getAttributes().getTranslationKey()).withStyle(form);
		tooltip.add(texComp);
		texComp = new StringTextComponent(fst.getAmount() + " mb");
		tooltip.add(texComp);		
	}

	@Override
	public net.minecraftforge.common.capabilities.ICapabilityProvider initCapabilities(ItemStack stack,
			@Nullable net.minecraft.nbt.CompoundNBT nbt) {
		fluidHandler = new FluidHandler(stack);
		return fluidHandler;
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		return fluidHandler.getCapability(cap);
	}

	public static class FluidHandler extends FluidHandlerItemStack {

		public FluidHandler(ItemStack container) {
			super(container, 1000); // bucket contains 1000 mb
		}

		protected void setFluid(FluidStack fluid) {
			super.setFluid(fluid);
			updateDamage(container);
		}

		private void updateDamage(ItemStack stack) {
			FluidHandler fst = new FluidHandler(stack);
			int containedLevels = fst.getFluid().getAmount() / Constants.MILLIBUCKETS_PER_LEVEL;
			stack.setDamageValue(Constants.MAX_FLUID_LEVEL - containedLevels);
		}
	}
}