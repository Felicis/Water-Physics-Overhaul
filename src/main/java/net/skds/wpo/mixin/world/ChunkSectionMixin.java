package net.skds.wpo.mixin.world;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.palette.IPalette;
import net.minecraft.util.palette.IdentityPalette;
import net.minecraft.util.palette.PalettedContainer;
import net.minecraft.world.chunk.ChunkSection;
import net.skds.wpo.mixininterfaces.ChunkSectionMixinInterface;
import net.skds.wpo.nbt.NBTUtilFluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ChunkSection modifications:<br/>
 * - fields:<br/>
 *   - ADDED nonEmptyFluidCount<br/>
 *   - ADDED GLOBAL_FLUIDSTATE_PALETTE + init<br/>
 *   - ADDED fluidStates<br/>
 * - methods:<br/>
 *   - MODIFIED constructor/init: init fields nonEmptyFluidCount, fluidStates<br/>
 *   - MODIFIED getFluidState - corrected<br/>
 *   - MODIFIED acquire/release: also acquire/release fluidStates Palette<br/>
 *   - ADDED: setFluidState x2 (code copied and adapted from setBlockState)<br/>
 *   - MODIFIED setBlockState: BlockState.getFluidState returns null (wrong use), FluidState.isEmpty returns true (nop => checked in setFluidState)<br/>
 *   - MODIFIED isEmpty: also check nonEmptyFluidCount<br/>
 *   - MODIFIED recalcBlockCounts: to compute separately from fields states and fluidStates and also use nonEmptyFluidCount<br/>
 *   - MODIFIED read/write/getSerializedSize: also read/write/count nonEmptyFluidCount and fluidstates<br/>
 *      => for now update BlockStates and FluidStates synchronously in chunk (if separate in future probably needs separate packets, handling,...)
 */
@Mixin(ChunkSection.class)
public abstract class ChunkSectionMixin implements ChunkSectionMixinInterface {
    @Shadow
    private short tickingFluidCount;
    @Shadow @Final private PalettedContainer<BlockState> states;
    @Shadow private short nonEmptyBlockCount;
    @Shadow private short tickingBlockCount;
    private short nonEmptyFluidCount;
    /**
     * TODO: check whether Fluid.FLUID_STATE_REGISTRY is safe to use like this or needs fix, too (check Block.BLOCK_STATE_REGISTRY with forge comment)
     * also check usages of block state registry and insert fluid as needed
     */
    private static final IPalette<FluidState> GLOBAL_FLUIDSTATE_PALETTE = new IdentityPalette<>(Fluid.FLUID_STATE_REGISTRY, Fluids.EMPTY.defaultFluidState());
    private PalettedContainer<FluidState> fluidStates; // final removed because IDE complained

    @Inject(method = "<init>(ISSS)V", at = @At(value = "RETURN"))
    private void initM(int pY, short pNonEmptyBlockCount, short pTickingBlockCount, short pTickingFluidCount, CallbackInfo ci) {
        this.nonEmptyFluidCount = (short) 0; // UPGRADE: adapt if constructor is called with non-zero init counts!
        this.fluidStates = new PalettedContainer<>(GLOBAL_FLUIDSTATE_PALETTE, Fluid.FLUID_STATE_REGISTRY,
                NBTUtilFluidState::readFluidState, NBTUtilFluidState::writeFluidState, Fluids.EMPTY.defaultFluidState());
    }

    /*
      Roadblock Injection
     */
    @Inject(method = "getFluidState", at = @At(value = "HEAD"), cancellable = true)
    public void getFluidState(int pX, int pY, int pZ, CallbackInfoReturnable<FluidState> cir) {
        cir.setReturnValue(this.fluidStates.get(pX, pY, pZ));
    }

    @Inject(method = "acquire", at = @At(value = "RETURN"))
    private void acquireEnd(CallbackInfo ci) {
        this.fluidStates.acquire();
    }

    @Inject(method = "release", at = @At(value = "RETURN"))
    private void releaseEnd(CallbackInfo ci) {
        this.fluidStates.release();
    }

    @Override
    public FluidState setFluidState(int pX, int pY, int pZ, FluidState pFluidState) {
        return this.setFluidState(pX, pY, pZ, pFluidState, true);
    }

    @Override
    public FluidState setFluidState(int pX, int pY, int pZ, FluidState pFluidState, boolean pUseLocks) {
        FluidState fluidState;
        if (pUseLocks) {
            fluidState = this.fluidStates.getAndSet(pX, pY, pZ, pFluidState);
        } else {
            fluidState = this.fluidStates.getAndSetUnchecked(pX, pY, pZ, pFluidState);
        }

        if (!fluidState.isEmpty()) {
            --this.tickingFluidCount;
        }

        if (!pFluidState.isEmpty()) {
            ++this.tickingFluidCount;
        }

        return fluidState;
    }

    @Redirect(method = "setBlockState(IIILnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/block/BlockState;getFluidState()Lnet/minecraft/fluid/FluidState;"))
    private FluidState getFluidStateNull(BlockState instance) {
        return null; // NOP: BlockState.getFluidState() should not be called. Will not crash because next mixin redirects FluidState.isEmpty
    }

    @Redirect(method = "setBlockState(IIILnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/fluid/FluidState;isEmpty()Z"))
    private boolean isEmptyNop(FluidState instance) {
        return true; // NOP: fluid ticks should not be updated when blockstate changes, but when fluidstate changes (possibly at the same time)
    }

    /*
      Roadblock Injection
     */
    @Inject(method = "isEmpty()Z", at = @At(value = "HEAD"), cancellable = true)
    private void isEmptyM(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(this.nonEmptyBlockCount == 0 && this.nonEmptyFluidCount == 0);
    }

    // separate counts into this.states and this.fluidStates
    @Inject(method = "recalcBlockCounts", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/palette/PalettedContainer;count(Lnet/minecraft/util/palette/PalettedContainer$ICountConsumer;)V"), cancellable = true)
    private void recalcBlockCountsM(CallbackInfo ci) {
        this.states.count((blockState, i) -> {
            if (!blockState.isAir()) {
                this.nonEmptyBlockCount = (short)(this.nonEmptyBlockCount + i);
                if (blockState.isRandomlyTicking()) {
                    this.tickingBlockCount = (short)(this.tickingBlockCount + i);
                }
            }
        });
        this.fluidStates.count((fluidState, i) -> {
            if (!fluidState.isEmpty()) {
                this.nonEmptyFluidCount = (short)(this.nonEmptyFluidCount + i);
                if (fluidState.isRandomlyTicking()) {
                    this.tickingFluidCount = (short)(this.tickingFluidCount + i);
                }
            }
        });
        ci.cancel();
    }


    @Override
    public PalettedContainer<FluidState> getFluidStates() {
        return this.fluidStates;
    }

    @Inject(method = "read", at = @At(value = "RETURN"))
    private void readM(PacketBuffer pPacketBuffer, CallbackInfo ci) {
        // after blockstates, read fluidstates (every read increases buffer => this is safe w/o knowing what states read); also array size included
        this.nonEmptyFluidCount = pPacketBuffer.readShort();
        this.fluidStates.read(pPacketBuffer);
    }

    @Inject(method = "write", at = @At(value = "RETURN"))
    private void writeM(PacketBuffer pPacketBuffer, CallbackInfo ci) {
        // after blockstates, write fluidstates (every write increases buffer => this is safe w/o knowing what states wrote); also array size included
        pPacketBuffer.writeShort(this.nonEmptyFluidCount);
        this.fluidStates.write(pPacketBuffer);
    }

    @Inject(method = "getSerializedSize", at = @At(value = "RETURN"), cancellable = true)
    private void getSerializedSizeM(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(cir.getReturnValue() + 2 + this.fluidStates.getSerializedSize());
    }
}
