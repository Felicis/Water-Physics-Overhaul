package net.skds.wpo.mixin.world;

import net.minecraft.world.IWorldWriter;
import net.skds.wpo.mixininterfaces.IWorldWriterMixinInterface;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(IWorldWriter.class)
public interface IWorldWriterMixin extends IWorldWriterMixinInterface {
}
