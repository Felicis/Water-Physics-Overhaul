package net.skds.wpo.mixin.world;

import net.minecraft.world.IWorld;
import net.skds.wpo.mixininterfaces.IWorldMixinInterface;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(IWorld.class)
public interface IWorldMixin extends IWorldMixinInterface {
}
