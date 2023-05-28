package net.skds.wpo.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.FlowingFluidBlock;
import net.skds.wpo.util.marker.WPOFluidMarker;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = {FlowingFluidBlock.class})
public abstract class FlowingFluidBlockMixin extends Block implements WPOFluidMarker {
    /**************+ MARKER MIXIN: do not remove this mixing even if empty ****************/

    public FlowingFluidBlockMixin(Properties p_i48440_1_) {
        super(p_i48440_1_);
    }
}