package net.skds.wpo.api;

import net.minecraft.util.Direction;

public interface IPressuredTank {
	public float getZeroPressure(Direction side);
	//public float getPressure();
	public float getPressure(Direction side);
	public void setPressure(float pressure, Direction side);
}