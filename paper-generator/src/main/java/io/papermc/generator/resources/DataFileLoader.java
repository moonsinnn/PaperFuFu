package io.papermc.generator.resources;

import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.squareup.javapoet.ClassName;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import io.papermc.generator.registry.RegistryEntries;
import io.papermc.generator.registry.RegistryEntry;
import io.papermc.generator.utils.ClassHelper;
import io.papermc.generator.utils.Formatting;
import io.papermc.generator.utils.SourceCodecs;
import io.papermc.generator.utils.predicate.BlockPredicate;
import io.papermc.generator.utils.predicate.ItemPredicate;
import io.papermc.typewriter.ClassNamed;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.vehicle.AbstractBoat;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jspecify.annotations.NullMarked;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@NullMarked
public class DataFileLoader {

    private static <K, V> Transmuter<Map<K, V>, Map.Entry<K, V>, K> sortedMap(Comparator<K> comparator) {
        return transmuteMap(MutationResult::constant, comparator);
    }

    private static <K, V> Transmuter<Map<K, V>, Map.Entry<K, V>, K> transmuteMap(Supplier<Set<K>> typesProvider, Function<K, V> onMissing, Comparator<K> comparator) {
        return transmuteMap(map -> {
            Set<K> types = typesProvider.get();
            Set<K> registeredTypes = map.keySet();

            Set<Map.Entry<K, V>> added = new HashSet<>();
            Sets.difference(types, registeredTypes).forEach(missingType -> added.add(Map.entry(missingType, onMissing.apply(missingType))));
            Set<K> removed = new HashSet<>(Sets.difference(registeredTypes, types));
            return new MutationResult<>(map, added, removed);
        }, comparator);
    }

    private static <K, V> Transmuter<Map<K, V>, Map.Entry<K, V>, K> transmuteMap(Function<Map<K, V>, io.papermc.generator.resources.MutationResult<Map<K, V>, Map.Entry<K, V>, K>> mutation, Comparator<K> comparator) {
        return new Transmuter.Mutable<>() {

            @Override
            public MutationResult<Map<K, V>, Map.Entry<K, V>, K> transmute(Map<K, V> value) {
                Map<K, V> map = new TreeMap<>(comparator);
                map.putAll(value);
                return mutation.apply(map);
            }

            @Override
            public Map<K, V> applyChanges(MutationResult<Map<K, V>, Map.Entry<K, V>, K> result) {
                result.added().forEach(entry -> result.value().put(entry.getKey(), entry.getValue()));
                result.removed().forEach(result.value()::remove);
                return result.value();
            }
        };
    }

    public static class BlockState {

        private static final Codec<Map<Class<? extends Enum<? extends StringRepresentable>>, ClassName>> ENUM_PROPERTY_TYPES_CODEC = Codec.unboundedMap(
            SourceCodecs.classCodec(new TypeToken<Enum<? extends StringRepresentable>>() {}), SourceCodecs.CLASS_NAME
        );
        public static final DataFile.Map<Class<? extends Enum<? extends StringRepresentable>>, ClassName> ENUM_PROPERTY_TYPES = new DataFile.Map<>(
            "data/block_state/enum_property_types.json", ENUM_PROPERTY_TYPES_CODEC,
            transmuteMap(() -> {
                try {
                    Set<Class<? extends Enum<? extends StringRepresentable>>> enumPropertyTypes = Collections.newSetFromMap(new IdentityHashMap<>());
                    for (Field field : BlockStateProperties.class.getDeclaredFields()) {
                        if (ClassHelper.isStaticConstant(field, Modifier.PUBLIC)) {
                            if (!EnumProperty.class.isAssignableFrom(field.getType())) {
                                continue;
                            }

                            enumPropertyTypes.add(((EnumProperty<?>) field.get(null)).getValueClass());
                        }
                    }
                    return Collections.unmodifiableSet(enumPropertyTypes);
                } catch (ReflectiveOperationException ex) {
                    throw new RuntimeException(ex);
                }
            },
            missingType -> ClassName.get("org.bukkit.block.data.type", missingType.getSimpleName()),
            Comparator.comparing(Class::getCanonicalName))
        );

        // add classes that extends MultipleFacing and RedstoneWire
        // no real rule here some has some don't mossy carpet and wall could have it
        private static final Codec<List<ClassNamed>> EXTRA_ALLOWED_METHOD_CODEC = SourceCodecs.CLASS_NAMED.listOf();
        public static final DataFile.List<ClassNamed> EXTRA_ALLOWED_METHOD = new DataFile.List<>(
            "data/block_state/extra_allowed_method.json", EXTRA_ALLOWED_METHOD_CODEC, MutationResult::constant
        );

        private static final Codec<Map<ClassNamed, List<BlockPredicate>>> PREDICATES_CODEC = Codec.unboundedMap(
            SourceCodecs.CLASS_NAMED, ExtraCodecs.nonEmptyList(BlockPredicate.CODEC.listOf())
        );
        public static final DataFile.Map<ClassNamed, List<BlockPredicate>> PREDICATES = new DataFile.Map<>(
            "data/block_state/predicates.json", PREDICATES_CODEC, MutationResult::constant, true
        );
    }

    @Deprecated
    public static class ItemMeta {
        public record BridgeData(ClassNamed api, String field) {

            public static final Codec<BridgeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                SourceCodecs.CLASS_NAMED.fieldOf("api").forGetter(BridgeData::api),
                SourceCodecs.IDENTIFIER.fieldOf("field").forGetter(BridgeData::field)
            ).apply(instance, BridgeData::new));
        }

        private static final Codec<Map<ClassNamed, BridgeData>> BRIDGE_CODEC = Codec.unboundedMap(SourceCodecs.CLASS_NAMED, BridgeData.CODEC);
        public static final DataFile.Map<ClassNamed, BridgeData> BRIDGE = new DataFile.Map<>(
            "data/item_meta/bridge.json", BRIDGE_CODEC, sortedMap(Comparator.comparing(ClassNamed::canonicalName))
        );

        private static final Codec<Map<ClassNamed, List<ItemPredicate>>> PREDICATES_CODEC = Codec.unboundedMap(SourceCodecs.CLASS_NAMED, ExtraCodecs.nonEmptyList(ItemPredicate.CODEC.listOf()));
        public static final DataFile.Map<ClassNamed, List<ItemPredicate>> PREDICATES = new DataFile.Map<>(
            "data/item_meta/predicates.json", PREDICATES_CODEC, MutationResult::constant, true
        );
    }

    private static final Codec<Map<ResourceKey<? extends Registry<?>>, RegistryData>> REGISTRIES_CODEC = Codec.unboundedMap(
        SourceCodecs.REGISTRY_KEY, RegistryData.CODEC
    );
    public static final Map<RegistryEntry.Type, DataFile.Map<ResourceKey<? extends Registry<?>>, RegistryData>> REGISTRIES = Collections.unmodifiableMap(Util.makeEnumMap(RegistryEntry.Type.class, type -> {
        return new DataFile.Map<>("data/registry/%s.json".formatted(type.getSerializedName()), REGISTRIES_CODEC, MutationResult::constant);
    }));

    private static final Map<ResourceKey<EntityType<?>>, Class<?>> ENTITY_TYPE_GENERICS = RegistryEntries.byRegistryKey(Registries.ENTITY_TYPE).getFields(field -> {
        if (field.getGenericType() instanceof ParameterizedType complexType && complexType.getActualTypeArguments().length == 1) {
            return (Class<?>) complexType.getActualTypeArguments()[0];
        }
        return null;
    });

    private static final Codec<Map<ResourceKey<EntityType<?>>, EntityTypeData>> ENTITY_TYPES_CODEC = Codec.unboundedMap(ResourceKey.codec(Registries.ENTITY_TYPE), EntityTypeData.CODEC);
    public static final DataFile.Map<ResourceKey<EntityType<?>>, EntityTypeData> ENTITY_TYPES = new DataFile.Map<>(
        "data/entity_types.json", ENTITY_TYPES_CODEC,
        transmuteMap(() -> BuiltInRegistries.ENTITY_TYPE.listElementIds().collect(Collectors.toSet()),
        missingType -> {
            Class<?> genericType = ENTITY_TYPE_GENERICS.get(missingType);

            String packageName = "org.bukkit.entity";
            if (AbstractBoat.class.isAssignableFrom(genericType)) {
                packageName += ".boat";
            } else if (AbstractMinecart.class.isAssignableFrom(genericType)) {
                packageName += ".minecart";
            }

            return new EntityTypeData(ClassNamed.of(packageName, genericType.getSimpleName()));
        },
        Formatting.alphabeticKeyOrder(key -> key.location().getPath()))
    );

    private static final Codec<Map<Class<? extends Mob>, ClassName>> ENTITY_CLASS_NAMES_CODEC = Codec.unboundedMap(
        SourceCodecs.classCodec(Mob.class), SourceCodecs.CLASS_NAME
    );
    public static final DataFile.Map<Class<? extends Mob>, ClassName> ENTITY_CLASS_NAMES = new DataFile.Map<>(
        "data/entity_class_names.json", ENTITY_CLASS_NAMES_CODEC,
        transmuteMap(() -> {
            try (ScanResult scanResult = new ClassGraph().enableClassInfo().acceptPackages(Entity.class.getPackageName()).scan()) {
                Set<Class<? extends Mob>> classes = new HashSet<>(scanResult.getSubclasses(Mob.class.getName()).loadClasses(Mob.class));
                if (classes.isEmpty()) {
                    throw new IllegalStateException("There are supposed to be more than 0 mob classes!");
                }

                classes.add(Mob.class);
                return Collections.unmodifiableSet(classes);
            }
        },
        missingType -> ClassName.get("org.bukkit.entity", missingType.getSimpleName()),
        Comparator.comparing(Class::getCanonicalName))
    );

    public static final @MonotonicNonNull List<DataFile<?, ?, ?>> DATA_FILES = Collections.unmodifiableList(Util.make(new ArrayList<>(), list -> {
        list.add(BlockState.ENUM_PROPERTY_TYPES);
        list.add(BlockState.EXTRA_ALLOWED_METHOD);
        list.add(BlockState.PREDICATES);
        list.add(ItemMeta.BRIDGE);
        list.add(ItemMeta.PREDICATES);
        list.addAll(REGISTRIES.values());
        list.add(ENTITY_CLASS_NAMES);
        list.add(ENTITY_TYPES);
    }));

    public static void init() {
    }
}
