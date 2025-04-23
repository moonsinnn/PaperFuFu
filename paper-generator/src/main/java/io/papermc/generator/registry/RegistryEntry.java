package io.papermc.generator.registry;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.papermc.generator.Main;
import io.papermc.generator.utils.ClassHelper;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class RegistryEntry<T> implements RegistryIdentifiable<T> {

    public static class Builder<T> implements RegistryIdentifiable<T> {

        private final ResourceKey<? extends Registry<T>> registryKey;
        private final Class<?> holderElementsClass;

        private @MonotonicNonNull RegistryKeyField<T> registryKeyField;
        private @MonotonicNonNull Type type;
        private @MonotonicNonNull RegistryData data;

        protected Builder(ResourceKey<? extends Registry<T>> registryKey, Class<?> holderElementsClass) {
            this.registryKey = registryKey;
            this.holderElementsClass = holderElementsClass;
        }

        @Override
        public ResourceKey<? extends Registry<T>> getRegistryKey() {
            return registryKey;
        }

        public Builder<T> registryKeyField(RegistryKeyField<T> registryKeyField) {
            this.registryKeyField = registryKeyField;
            return this;
        }

        public Builder<T> type(Type type) {
            this.type = type;
            return this;
        }

        public Builder<T> data(RegistryData data) {
            this.data = data;
            return this;
        }

        public RegistryEntry<T> build() {
            Preconditions.checkState(this.type != null && this.data != null && this.registryKeyField != null, "Type, data and registry key field cannot be nulls");
            return new RegistryEntry<>(this.registryKey, this.registryKeyField, this.holderElementsClass, this.type, this.data);
        }
    }

    private final ResourceKey<? extends Registry<T>> registryKey;
    private final RegistryKeyField<T> registryKeyField;
    private final Class<T> elementClass;
    private final Class<?> holderElementsClass;
    private final Type type;
    private final RegistryData data;

    private @Nullable Map<ResourceKey<T>, String> fieldNames;

    private RegistryEntry(ResourceKey<? extends Registry<T>> registryKey, RegistryKeyField<T> registryKeyField, Class<?> holderElementsClass, Type type, RegistryData data) {
        this.registryKey = registryKey;
        this.registryKeyField = registryKeyField;
        this.elementClass = registryKeyField.elementClass();
        this.holderElementsClass = holderElementsClass;
        this.type = type;
        this.data = data;
    }

    @Override
    public ResourceKey<? extends Registry<T>> getRegistryKey() {
        return this.registryKey;
    }

    public Registry<T> registry() {
        return Main.REGISTRY_ACCESS.lookupOrThrow(this.registryKey);
    }

    public Class<T> elementClass() {
        return this.elementClass;
    }

    public String registryKeyField() {
        return this.registryKeyField.name();
    }

    public RegistryData data() {
        return this.data;
    }

    public void validate() {
        boolean isBuiltIn = this.type == Type.BUILT_IN;
        Type realType = this.type == Type.BUILT_IN ? Type.DATA_DRIVEN : Type.BUILT_IN;
        Preconditions.checkState(isBuiltIn == BuiltInRegistries.REGISTRY.containsKey(this.registryKey.location()), // type is checked at runtime and not guessed in case api/impl change is needed
            "Mismatch type, registry %s is %s but was in registry/%s.json".formatted(this.registryKey.location(), realType.getSerializedName(), this.type.getSerializedName())
        );
    }

    public String keyClassName() {
        if (this.data.api().keyClassNameRelate()) {
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

    public enum Type implements StringRepresentable {

        DATA_DRIVEN("data_driven", RegistryEntries.DATA_DRIVEN),
        BUILT_IN("built_in", RegistryEntries.BUILT_IN, RegistryData.UNSAFE_CODEC.validate(data -> {
            return data.impl().delayed() ? DataResult.error(() -> "Built-in registry cannot be delayed!") : DataResult.success(data);
        }));

        private final String name;
        private final List<RegistryEntry<?>> entries;
        private final Codec<RegistryData> dataCodec;

        Type(String name, List<RegistryEntry<?>> entries) {
            this(name, entries, RegistryData.UNSAFE_CODEC);
        }

        Type(String name, List<RegistryEntry<?>> entries, Codec<RegistryData> dataCodec) {
            this.name = name;
            this.entries = entries;
            this.dataCodec = dataCodec;
        }

        public List<RegistryEntry<?>> getEntries() {
            return this.entries;
        }

        public Codec<RegistryData> getDataCodec() {
            return this.dataCodec;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
