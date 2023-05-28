package net.skds.wpo.block.tileentity;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.skds.wpo.fluidphysics.FFluidStatic;
import net.skds.wpo.util.api.IPressuredTank;

public abstract class BasicTankEntity extends TileEntity implements ITickableTileEntity, IPressuredTank, IFluidHandler {

	public int capacity = 500;
	protected static final float ATM_PRESSURE = 1f;

	protected FluidTank tank;
	
	public BasicTankEntity(TileEntityType<?> tileEntityTypeIn) {
		super(tileEntityTypeIn);
		tank = new FluidTank(capacity);
	}

	public BasicTankEntity(TileEntityType<?> tileEntityTypeIn, int capacity) {
		super(tileEntityTypeIn);
		this.capacity = capacity;
		tank = new FluidTank(capacity);
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (tank.isEmpty()) {
			return;
		}
		FlowingFluid tf = (FlowingFluid) tank.getFluid().getFluid();
		BlockState state0 = level.getBlockState(worldPosition);
		int lvl = tank.getFluid().getAmount() / 125;
		FluidState fs = state0.getFluidState();
		if (!fs.isEmpty() && fs.getType().isSame(tf)) {
			lvl += fs.getAmount();
			if (lvl > 8) {
				lvl = 8;
			}
		} else {
			state0 = Blocks.AIR.defaultBlockState();
		}
		//System.out.println(state0);
		BlockState ns = FFluidStatic.forceApplyFluid(state0, lvl, tf);
		level.setBlockAndUpdate(worldPosition, ns);
	}	
}