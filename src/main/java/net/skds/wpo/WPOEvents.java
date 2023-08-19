package net.skds.wpo;

import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.PistonEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.skds.wpo.fluidphysics.EventStatic;

public class WPOEvents {

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onPrePistonEvent(PistonEvent.Pre e) {
		EventStatic.onPistonPre(e);
	}

	@SubscribeEvent
	public void onBucketEvent(FillBucketEvent e) {
		EventStatic.onBucketEvent(e);
	}

	@SubscribeEvent
	public void onBlockPlaceEvent(BlockEvent.EntityPlaceEvent e) {
		EventStatic.onBlockPlace(e);
	}
}