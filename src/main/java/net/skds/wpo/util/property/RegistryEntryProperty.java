package net.skds.wpo.util.property;

import net.minecraft.state.Property;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;

import java.util.Collection;
import java.util.Optional;

/**
 * Property to store ResourceLocation in BlockState (e.g. Fluid type)
 *
 * @param <T>
 */
public class RegistryEntryProperty<T> extends Property<ResourceLocation> {
    private final Collection<ResourceLocation> values;

    public RegistryEntryProperty(String name, Registry<T> registry) {
        super(name, ResourceLocation.class);
        this.values = registry.keySet();
    }

    @Override
    public Collection<ResourceLocation> getPossibleValues() {
        return this.values;
    }

    @Override
    public Optional<ResourceLocation> getValue(String valueName) {
        ResourceLocation resourceLocation = new ResourceLocation(valueName);
        if (this.values.contains(resourceLocation)) {
            return Optional.of(resourceLocation);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String getName(ResourceLocation pValue) {
        return pValue.toString();
    }
}
