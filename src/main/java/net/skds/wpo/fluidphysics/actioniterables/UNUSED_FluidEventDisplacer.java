package net.skds.wpo.fluidphysics.actioniterables;

import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.Event;

/**
 * just like the FluidDisplacer, only started from an event that has to be finished
 */
public class UNUSED_FluidEventDisplacer extends FluidDisplacer {
    BlockEvent.EntityPlaceEvent event;
    public UNUSED_FluidEventDisplacer(World w, BlockEvent.EntityPlaceEvent e) {
        super(w, e.getBlockSnapshot().getReplacedBlock());
        event = e;
    }

    @Override
    public void finish() {
        super.finish();
        event.setResult(Event.Result.ALLOW);
    }
}
