package io.papermc.generator.registry;

import io.papermc.generator.Main;
import io.papermc.generator.utils.ClassHelper;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class RegistryEntry<T> {

    private final ResourceKey<? extends Registry<T>> registryKey;
    private final RegistryKeyField<T> registryKeyField;
    private final Class<T> elementClass;
    private final Class<?> holderElementsClass;
    private final RegistryData data;

    private @Nullable Map<ResourceKey<T>, String> fieldNames;

    public RegistryEntry(ResourceKey<? extends Registry<T>> registryKey, RegistryKeyField<T> registryKeyField, Class<?> holderElementsClass, RegistryData data) {
        this.registryKey = registryKey;
        this.registryKeyField = registryKeyField;
        this.elementClass = registryKeyField.elementClass();
        this.holderElementsClass = holderElementsClass;
        this.data = data;
    }

    public ResourceKey<? extends Registry<T>> registryKey() {
        return this.registryKey;
    }

    public Registry<T> registry() {
        return Main.REGISTRY_ACCESS.lookupOrThrow(this.registryKey);
    }

    public String registryKeyField() {
        return this.registryKeyField.name();
    }

    public RegistryData data() {
        return this.data;
    }

    public String keyClassName() {
        if (this.data.keyClassNameBasedOnApi()) {
            return this.data.api().klass().simpleName();
        }

        return this.elementClass.getSimpleName();
    }

    public boolean allowCustomKeys() {
        return this.data.builder().isPresent() || RegistryEntries.DATA_DRIVEN.contains(this);
    }

    private <TO> Map<ResourceKey<T>, TO> getFields(Map<ResourceKey<T>, TO> map, Function<Field, @Nullable TO> transform) {
        Registry<T> registry = this.registry();
        try {
            for (Field field : this.holderElementsClass.getDeclaredFields()) {
                if (!ResourceKey.class.isAssignableFrom(field.getType()) && !Holder.Reference.class.isAssignableFrom(field.getType()) && !this.elementClass.isAssignableFrom(field.getType())) {
                    continue;
                }

                if (ClassHelper.isStaticConstant(field, Modifier.PUBLIC)) {
                    ResourceKey<T> key = null;
                    if (this.elementClass.isAssignableFrom(field.getType())) {
                        key = registry.getResourceKey(this.elementClass.cast(field.get(null))).orElseThrow();
                    } else {
                        if (field.getGenericType() instanceof ParameterizedType complexType && complexType.getActualTypeArguments().length == 1 &&
                            complexType.getActualTypeArguments()[0] == this.elementClass) {

                            if (Holder.Reference.class.isAssignableFrom(field.getType())) {
                                key = ((Holder.Reference<T>) field.get(null)).key();
                            } else {
                                key = (ResourceKey<T>) field.get(null);
                            }
                        }
                    }
                    if (key != null) {
                        TO value = transform.apply(field);
                        if (value != null) {
                            map.put(key, value);
                        }
                    }
                }
            }
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
        return map;
    }

    public Map<ResourceKey<T>, String> getFieldNames() {
        if (this.fieldNames == null) {
            this.fieldNames = this.getFields(Field::getName);
        }
        return this.fieldNames;
    }

    public <TO> Map<ResourceKey<T>, TO> getFields(Function<Field, @Nullable TO> transform) {
        return Collections.unmodifiableMap(this.getFields(new IdentityHashMap<>(), transform));
    }

    @Override
    public String toString() {
        return "RegistryEntry[" +
            "registryKey=" + this.registryKey + ", " +
            "registryKeyField=" + this.registryKeyField + ", " +
            "data=" + this.data +
            ']';
    }
}
