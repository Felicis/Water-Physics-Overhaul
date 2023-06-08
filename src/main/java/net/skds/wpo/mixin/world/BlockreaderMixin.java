package net.skds.wpo.mixin.world;

import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Blockreader;
import net.minecraft.world.IBlockReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Blockreader.class)
public abstract class BlockreaderMixin implements BlockReaderMixinInterface, IBlockReader {
    private FluidState[] fluidColumn; // cannot do final, because changed outside constructor

    /**
     * used to add fluidstates after constructor call (as MixinInterface)
     * UPGRADE: check for new constructor calls and inject call of this:
     *      use FFluidStatic.getDefaultFluidState(BlockState) to get FluidState from BlockState
     * @param fstates
     */
    public void addFluidStates(FluidState[] fstates) {
        this.fluidColumn = fstates;
    }

    /*
      Roadblock Injection
     */
    @Inject(method = "getFluidState", at = @At(value = "HEAD"), cancellable = true)
    private void getFluidStateM(BlockPos pPos, CallbackInfoReturnable<FluidState> cir) {
        int i = pPos.getY();
        cir.setReturnValue(i >= 0 && i < this.fluidColumn.length ? this.fluidColumn[i] : Fluids.EMPTY.defaultFluidState());
    }
}
