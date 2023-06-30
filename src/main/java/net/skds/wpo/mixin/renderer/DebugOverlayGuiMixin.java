package net.skds.wpo.mixin.renderer;

import net.minecraft.block.BlockState;
import net.minecraft.client.gui.overlay.DebugOverlayGui;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DebugOverlayGui.class)
public class DebugOverlayGuiMixin {
    /* DEBUGGING HELP: show blockstate of fluid */
    private BlockState blockStateOfFluid; // cache block state between redirects

    @Redirect(method = "getSystemInformation", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;getFluidState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/fluid/FluidState;"))
    private FluidState getFluidState_alsoGetBlockState(ClientWorld instance, BlockPos pos) {
        blockStateOfFluid = instance.getBlockState(pos); // cache block state
        return instance.getFluidState(pos); // original redirected code
    }

    @Redirect(method = "getSystemInformation", at = @At(value = "INVOKE", target = "Ljava/lang/String;valueOf(Ljava/lang/Object;)Ljava/lang/String;", ordinal = 1))
    private String add_injectFluidBlockState(Object o) {
        String fluidType = String.valueOf(o);
        String blockType = String.valueOf((Object) Registry.BLOCK.getKey(blockStateOfFluid.getBlock()));
        return fluidType + " (" + blockType + ")";
    }
}
