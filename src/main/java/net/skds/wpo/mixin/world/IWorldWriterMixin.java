package net.skds.wpo.mixin.world;

import net.minecraft.world.IWorldWriter;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(IWorldWriter.class)
public interface IWorldWriterMixin extends IWorldWriterMixinInterface {
}
