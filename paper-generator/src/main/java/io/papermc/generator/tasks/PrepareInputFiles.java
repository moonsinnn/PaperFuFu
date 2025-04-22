package io.papermc.generator.tasks;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.squareup.javapoet.ClassName;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import io.papermc.generator.Main;
import io.papermc.generator.registry.RegistryEntries;
import io.papermc.generator.rewriter.types.simple.EntityTypeRewriter;
import io.papermc.generator.types.goal.MobGoalNames;
import io.papermc.generator.utils.BlockStateMapping;
import io.papermc.generator.utils.ClassHelper;
import io.papermc.generator.utils.Formatting;
import io.papermc.generator.utils.ItemMetaData;
import io.papermc.generator.utils.SourceCodecs;
import io.papermc.typewriter.ClassNamed;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.slf4j.Logger;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

public class PrepareInputFiles {

    static {
        Main.bootStrap(true);
    }

    private static final Logger LOGGER = LogUtils.getLogger();

    private interface MutationResult<A> {
        A value();

        Set<?> added();

        Set<?> removed();
    }

    private record SimpleMutationResult<A>(A value, Set<?> added, Set<?> removed) implements MutationResult<A> {

        public static <A> MutationResult<A> noMutation(A value) {
            return new SimpleMutationResult<>(value, Collections.emptySet(), Collections.emptySet());
        }
    }

    private record MapMutationResult<K, V>(Map<K, V> value, Set<Map.Entry<K, V>> added, Set<K> removed) implements MutationResult<Map<K, V>> {
    }

    @FunctionalInterface
    private interface Transmuter<A> {

        MutationResult<A> transmute(A value);
    }

    private record DataFile<A>(String path, Codec<A> codec, Transmuter<A> transmuter) {

        public MutationResult<A> transmuteRaw(Object value) {
            return this.transmuter.transmute((A) value);
        }
    }

    private static <K, V> Transmuter<Map<K, V>> transmuteMap(Comparator<? super K> comparator) {
        return transmuteMap(SimpleMutationResult::noMutation, comparator);
    }

    private static <K, V> Transmuter<Map<K, V>> transmuteDeltaMap(Supplier<Set<K>> typesProvider, Function<K, V> onMissing, Comparator<? super K> comparator) {
        return transmuteMap(map -> {
            Set<K> types = typesProvider.get();
            Set<K> registeredTypes = map.keySet();

            Set<Map.Entry<K, V>> added = new HashSet<>();
            Sets.difference(types, registeredTypes).forEach(missingType -> added.add(Map.entry(missingType, onMissing.apply(missingType))));
            Set<K> removed = new HashSet<>(Sets.difference(registeredTypes, types));

            added.forEach(entry -> map.put(entry.getKey(), entry.getValue()));
            removed.forEach(map::remove);
            return new MapMutationResult<>(map, added, removed);
        }, comparator);
    }

    private static <K, V> Transmuter<Map<K, V>> transmuteMap(Function<Map<K, V>, MutationResult<Map<K, V>>> mutation, Comparator<? super K> comparator) {
        return value -> {
            TreeMap<K, V> map = new TreeMap<>(comparator);
            map.putAll(value);
            return mutation.apply(map);
        };
    }

    private static final Map<ResourceKey<EntityType<?>>, Class<?>> ENTITY_TYPE_GENERICS = RegistryEntries.byRegistryKey(Registries.ENTITY_TYPE).getFields(field -> {
        if (field.getGenericType() instanceof ParameterizedType complexType && complexType.getActualTypeArguments().length == 1) {
            return (Class<?>) complexType.getActualTypeArguments()[0];
        }
        return null;
    });

    private static final List<DataFile<?>> DATA_FILES = List.of(
        new DataFile<>("enum_property_types.json", BlockStateMapping.ENUM_PROPERTY_TYPES_CODEC, transmuteDeltaMap(() -> {
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
        ),
        new DataFile<>("entity_types.json", EntityTypeRewriter.DATA_CODEC, transmuteDeltaMap(() -> BuiltInRegistries.ENTITY_TYPE.listElementIds().collect(Collectors.toSet()),
            missingType -> new EntityTypeRewriter.Data(ClassNamed.of("org.bukkit.entity", ENTITY_TYPE_GENERICS.get(missingType).getSimpleName())),
            Formatting.alphabeticKeyOrder(key -> key.location().getPath()))
        ),
        new DataFile<>("entity_class_names.json", MobGoalNames.ENTITY_CLASS_NAMES_CODEC, transmuteDeltaMap(() -> {
                try (ScanResult scanResult = new ClassGraph().enableClassInfo().whitelistPackages(Entity.class.getPackageName()).scan()) {
                    Set<Class<? extends Mob>> classes = new HashSet<>(scanResult.getSubclasses(Mob.class.getName()).loadClasses(Mob.class));
                    classes.add(Mob.class);
                    return Collections.unmodifiableSet(classes);
                }
            },
            missingType -> ClassName.get("org.bukkit.entity", missingType.getSimpleName()),
            Comparator.comparing(Class::getCanonicalName))
        ),
        new DataFile<>("item_meta/bridge.json", ItemMetaData.BRIDGE_CODEC, transmuteMap(Comparator.comparing(ClassNamed::canonicalName)))
    );

    public static void main(String[] args) throws IOException {
        Path resourceDir = Path.of(args[0]);
        for (DataFile<?> file : DATA_FILES) {
            Path filePath = Path.of("data", file.path());
            Path resourcePath = resourceDir.resolve(filePath);
            try (Reader input = Files.newBufferedReader(resourcePath, StandardCharsets.UTF_8)) {
                JsonElement element = SourceCodecs.GSON.fromJson(input, JsonElement.class);
                MutationResult<?> result = file.transmuteRaw(file.codec().parse(JsonOps.INSTANCE, element).getOrThrow());
                element = ((Codec<Object>) file.codec()).encodeStart(JsonOps.INSTANCE, result.value()).getOrThrow();
                Files.writeString(resourcePath, SourceCodecs.GSON.toJson(element) + "\n", StandardCharsets.UTF_8);

                if (!result.added().isEmpty()) {
                    LOGGER.info("Added the following elements in {}:", filePath);
                    result.added().stream().map(Object::toString).forEach(LOGGER::info);
                }
                if (!result.removed().isEmpty()) {
                    LOGGER.warn("Removed the following keys in {}:", filePath);
                    result.removed().stream().map(Object::toString).forEach(LOGGER::info);
                }
            }
        }
    }
}
