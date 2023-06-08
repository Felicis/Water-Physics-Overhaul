package net.skds.wpo.mixin.world;

import net.minecraft.world.IWorld;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(IWorld.class)
public interface IWorldMixin extends IWorldMixinInterface {
}
