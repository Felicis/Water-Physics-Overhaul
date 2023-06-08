package net.skds.wpo.util.nbt;

import com.google.common.collect.ImmutableMap;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.Property;
import net.minecraft.state.StateContainer;
import net.minecraft.state.StateHolder;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Optional;

public final class NBTUtilFluidState {
    // UPGRADE: copied from NBTUtil.java with changes
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Reads a fluidstate from the given tag.
     */
    public static FluidState readFluidState(CompoundNBT pTag) {
        if (!pTag.contains("Name", 8)) {
            return Fluids.EMPTY.defaultFluidState(); // CHANGE
        } else {
            Fluid fluid = Registry.FLUID.get(new ResourceLocation(pTag.getString("Name"))); // CHANGE
            FluidState fluidState = fluid.defaultFluidState(); // CHANGE
            if (pTag.contains("Properties", 10)) {
                CompoundNBT compoundnbt = pTag.getCompound("Properties");
                StateContainer<Fluid, FluidState> statecontainer = fluid.getStateDefinition(); // CHANGE

                for (String s : compoundnbt.getAllKeys()) {
                    Property<?> property = statecontainer.getProperty(s);
                    if (property != null) {
                        fluidState = setValueHelper(fluidState, property, s, compoundnbt, pTag); // CHANGE
                    }
                }
            }

            return fluidState; // CHANGE
        }
    }

    // copied 1=1, only LOGGER msg text changed (blockstate -> fluidstate)
    private static <S extends StateHolder<?, S>, T extends Comparable<T>> S setValueHelper(S pStateHolder, Property<T> pProperty, String pPropertyName, CompoundNBT pPropertiesTag, CompoundNBT pBlockStateTag) {
        Optional<T> optional = pProperty.getValue(pPropertiesTag.getString(pPropertyName));
        if (optional.isPresent()) {
            return pStateHolder.setValue(pProperty, optional.get());
        } else {
            LOGGER.warn("Unable to read property: {} with value: {} for fluidstate: {}", pPropertyName, pPropertiesTag.getString(pPropertyName), pBlockStateTag.toString());
            return pStateHolder;
        }
    }


    /**
     * Writes the given blockstate to the given tag.
     */
    public static CompoundNBT writeFluidState(FluidState pFluidState) {
        CompoundNBT compoundnbt = new CompoundNBT();
        compoundnbt.putString("Name", Registry.FLUID.getKey(pFluidState.getType()).toString()); // CHANGE
        ImmutableMap<Property<?>, Comparable<?>> immutablemap = pFluidState.getValues(); // CHANGE
        if (!immutablemap.isEmpty()) {
            CompoundNBT compoundnbt1 = new CompoundNBT();

            for (Map.Entry<Property<?>, Comparable<?>> entry : immutablemap.entrySet()) {
                Property<?> property = entry.getKey();
                compoundnbt1.putString(property.getName(), getName(property, entry.getValue()));
            }

            compoundnbt.put("Properties", compoundnbt1);
        }

        return compoundnbt;
    }

    // copied 1=1
    private static <T extends Comparable<T>> String getName(Property<T> pProperty, Comparable<?> pValue) {
        return pProperty.getName((T) pValue);
    }
}
