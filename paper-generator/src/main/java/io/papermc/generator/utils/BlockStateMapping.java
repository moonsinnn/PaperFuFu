package io.papermc.generator.utils;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.papermc.generator.types.craftblockdata.property.holder.VirtualField;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import io.papermc.typewriter.ClassNamed;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.StructureBlock;
import net.minecraft.world.level.block.TestBlock;
import net.minecraft.world.level.block.TestInstanceBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
/*
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.type.Dripleaf;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.Furnace;
import org.bukkit.block.data.type.RedstoneRail;
import org.bukkit.block.data.type.ResinClump;
import org.bukkit.block.data.type.Switch;*/
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class BlockStateMapping {

    public record BlockData(String implName, @Nullable Class<?> api,
                            Collection<? extends Property<?>> properties, Map<Property<?>, Field> propertyFields,
                            Multimap<Either<Field, VirtualField>, Property<?>> complexPropertyFields) {
    }
    /*
    public record BlockData(String implName, @Nullable Class<? extends org.bukkit.block.data.BlockData> api,
                            Collection<? extends Property<?>> properties, Map<Property<?>, Field> propertyFields,
                            Multimap<Either<Field, VirtualField>, Property<?>> complexPropertyFields) {
    }*/

    private static final Map<String, String> API_RENAMES = ImmutableMap.<String, String>builder()
        .put("SnowLayer", "Snow")
        .put("StainedGlassPane", "GlassPane") // weird that this one implements glass pane but not the regular glass pane
        .put("CeilingHangingSign", "HangingSign")
        .put("RedStoneWire", "RedstoneWire")
        .put("TripWire", "Tripwire")
        .put("TripWireHook", "TripwireHook")
        .put("Tnt", "TNT")
        .put("BambooStalk", "Bamboo")
        .put("Farm", "Farmland")
        .put("ChiseledBookShelf", "ChiseledBookshelf")
        .put("UntintedParticleLeaves", "Leaves")
        .put("TintedParticleLeaves", "Leaves")
        .put("StandingSign", "Sign")
        .put("FenceGate", "Gate")
        .buildOrThrow();

    private static final Set<Class<? extends Block>> BLOCK_SUFFIX_INTENDED = Set.of(
        CommandBlock.class,
        StructureBlock.class,
        NoteBlock.class,
        TestBlock.class,
        TestInstanceBlock.class
    );

    // virtual data that doesn't exist as constant in the source but still organized this way in the api
    public static final ImmutableMultimap<Class<?>, VirtualField> VIRTUAL_NODES = ImmutableMultimap.<Class<?>, VirtualField>builder()
        .build();

    public static final BiMap<Property<?>, Field> FALLBACK_GENERIC_FIELDS;

    static {
        ImmutableBiMap.Builder<Property<?>, Field> fallbackGenericFields = ImmutableBiMap.builder();
        fetchProperties(BlockStateProperties.class, (field, property) -> fallbackGenericFields.put(property, field), null);
        FALLBACK_GENERIC_FIELDS = fallbackGenericFields.buildOrThrow();
    }

    /*
    public static final Map<Class<? extends Block>, BlockData> MAPPING;

    static {
        Map<Class<? extends Block>, Collection<Property<?>>> specialBlocks = new IdentityHashMap<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!block.getStateDefinition().getProperties().isEmpty()) {
                specialBlocks.put(block.getClass(), block.getStateDefinition().getProperties());
            }
        }

        Map<Class<? extends Block>, BlockData> map = new IdentityHashMap<>();
        for (Map.Entry<Class<? extends Block>, Collection<Property<?>>> entry : specialBlocks.entrySet()) {
            Class<? extends Block> specialBlock = entry.getKey();

            Collection<Property<?>> properties = new ArrayList<>(entry.getValue());

            Map<Property<?>, Field> propertyFields = new HashMap<>(properties.size());
            Multimap<Either<Field, VirtualField>, Property<?>> complexPropertyFields = ArrayListMultimap.create();

            fetchProperties(specialBlock, (field, property) -> {
                if (properties.contains(property)) {
                    propertyFields.put(property, field);
                }
            }, (field, property) -> {
                if (properties.remove(property)) { // handle those separately and only count if the property was in the state definition
                    complexPropertyFields.put(Either.left(field), property);
                }
            });

            // virtual nodes
            if (VIRTUAL_NODES.containsKey(specialBlock)) {
                for (VirtualField virtualField : VIRTUAL_NODES.get(specialBlock)) {
                    for (Property<?> property : virtualField.values()) {
                        if (properties.remove(property)) {
                            complexPropertyFields.put(Either.right(virtualField), property);
                        } else {
                            throw new IllegalStateException("Unhandled virtual node " + virtualField.name() + " for " + property + " in " + specialBlock.getCanonicalName());
                        }
                    }
                }
            }

            String apiName = formatApiName(specialBlock);
            String implName = "Craft".concat(apiName); // before renames

            apiName = Formatting.stripWordOfCamelCaseName(apiName, "Base", true);
            apiName = API_RENAMES.getOrDefault(apiName, apiName);

            Class<? extends org.bukkit.block.data.BlockData> api = ClassHelper.classOr("org.bukkit.block.data.type." + apiName, null);
            if (api == null) {
                Class<?> directParent = specialBlock.getSuperclass();
                if (specialBlocks.containsKey(directParent)) {
                    // if the properties are the same then always consider the parent
                    // check deeper in the tree?
                    if (specialBlocks.get(directParent).equals(entry.getValue())) {
                        String parentApiName = formatApiName(directParent);
                        parentApiName = Formatting.stripWordOfCamelCaseName(parentApiName, "Base", true);
                        parentApiName = API_RENAMES.getOrDefault(parentApiName, parentApiName);
                        api = ClassHelper.classOr("org.bukkit.block.data.type." + parentApiName, api);
                    }
                }
            }
            if (api == null) { // todo remove this part
                if (AbstractFurnaceBlock.class.isAssignableFrom(specialBlock)) {
                    api = Furnace.class; // for smoker and blast furnace
                } else if (specialBlock == BigDripleafStemBlock.class) {
                    api = Dripleaf.class;
                } else if (specialBlock == IronBarsBlock.class) {
                    api = Fence.class; // for glass pane (regular) and iron bars
                } else if (specialBlock == MultifaceBlock.class) {
                    api = ResinClump.class;
                }
            }

            map.put(specialBlock, new BlockData(implName, api, properties, propertyFields, complexPropertyFields));
        }
        MAPPING = Collections.unmodifiableMap(map);
    }*/

    public record PropertyData(Optional<String> field, Optional<String> name, ClassNamed api, boolean pure) implements Comparable<PropertyData> { // todo either? fieldOrName but keep them separate in the codec

        public static final Codec<PropertyData> UNSAFE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SourceCodecs.IDENTIFIER.optionalFieldOf("field").forGetter(PropertyData::field),
            Codec.STRING.optionalFieldOf("name").forGetter(PropertyData::name),
            SourceCodecs.CLASS_NAMED.fieldOf("api").forGetter(PropertyData::api),
            Codec.BOOL.optionalFieldOf("pure", false).forGetter(PropertyData::pure)
        ).apply(instance, PropertyData::new));

        public static final Codec<PropertyData> CODEC = UNSAFE_CODEC.validate(data -> {
            return data.field().isEmpty() && data.name().isEmpty() ? DataResult.error(() -> "Must contains a name or a field") : DataResult.success(data);
        });

        @Override
        public int compareTo(@NotNull BlockStateMapping.PropertyData data) {
            if (this.field().isPresent() && data.field().isPresent()) {
                return this.field().orElseThrow().compareTo(data.field().orElseThrow());
            } else if (this.name().isPresent() && data.name().isPresent()) {
                return this.name().orElseThrow().compareTo(data.name().orElseThrow());
            } else {
                if (this.name().isPresent() && data.field().isPresent()) {
                    return 1;
                } else if (this.field().isPresent() && data.name().isPresent()) {
                    return -1;
                }

                return 0; // shouldn't happen
            }
        }
    }

    // levelled and ageable are done using the property name
    // multiple facing is done by matching two or more pipe block properties

    private static final Map<String, ClassNamed> NAME_TO_DATA;
    private static final Map<Property<?>, ClassNamed> PROPERTY_TO_DATA;
    private static final Map<Property<?>, ClassNamed> MAIN_PROPERTY_TO_DATA;
    public static final Codec<List<PropertyData>> PROPERTY_DATA_CODEC = PropertyData.CODEC.listOf();
    static {
        List<PropertyData> propertyData;
        try (Reader input = new BufferedReader(new InputStreamReader(BlockStateMapping.class.getClassLoader().getResourceAsStream("data/block_state_properties.json")))) {
            JsonArray properties = SourceCodecs.GSON.fromJson(input, JsonArray.class);
            propertyData = PROPERTY_DATA_CODEC.parse(JsonOps.INSTANCE, properties).getOrThrow();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        Map<String, Property<?>> propertyByName = FALLBACK_GENERIC_FIELDS.inverse().entrySet().stream()
            .collect(Collectors.toMap(entry -> entry.getKey().getName(), Map.Entry::getValue));

        NAME_TO_DATA = propertyData.stream().filter(data -> !data.pure() && data.name().isPresent())
            .collect(Collectors.toUnmodifiableMap(data -> data.name().get(), PropertyData::api));
        MAIN_PROPERTY_TO_DATA = propertyData.stream().filter(data -> data.pure() && data.field().isPresent())
            .collect(Collectors.toUnmodifiableMap(data -> propertyByName.get(data.field().get()), PropertyData::api));
        PROPERTY_TO_DATA = propertyData.stream().filter(data -> !data.pure() && data.field().isPresent())
            .collect(Collectors.toUnmodifiableMap(data -> propertyByName.get(data.field().get()), PropertyData::api));
    }

    public static final Map<Class<? extends Enum<? extends StringRepresentable>>, ClassNamed> ENUM_PROPERTY_TYPES;
    public static final Codec<Map<Class<? extends Enum<? extends StringRepresentable>>, ClassNamed>> ENUM_PROPERTY_TYPES_CODEC = Codec.unboundedMap(
        SourceCodecs.classCodec(new TypeToken<Enum<? extends StringRepresentable>>() {}), SourceCodecs.CLASS_NAMED
    );

    static {
        try (Reader input = new BufferedReader(new InputStreamReader(BlockStateMapping.class.getClassLoader().getResourceAsStream("data/enum_property_types.json")))) {
            JsonObject properties = SourceCodecs.GSON.fromJson(input, JsonObject.class);
            ENUM_PROPERTY_TYPES = ENUM_PROPERTY_TYPES_CODEC.parse(JsonOps.INSTANCE, properties).getOrThrow();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /*
    public static @Nullable ClassNamed getBestSuitedApiClass(Class<?> block) {
        if (!MAPPING.containsKey(block)) {
            return null;
        }

        return getBestSuitedApiClass(MAPPING.get(block));
    }

    public static @Nullable ClassNamed getBestSuitedApiClass(BlockData data) {
        if (data.api() != null) {
            return data.api();
        }

        int pipeProps = 0;
        Set<ClassNamed> extensions = new LinkedHashSet<>();
        for (Property<?> property : data.properties()) {
            if (MAIN_PROPERTY_TO_DATA.containsKey(property)) {
                return MAIN_PROPERTY_TO_DATA.get(property);
            }

            if (NAME_TO_DATA.containsKey(property.getName())) {
                extensions.add(NAME_TO_DATA.get(property.getName()));
                continue;
            }

            if (PROPERTY_TO_DATA.containsKey(property)) {
                extensions.add(PROPERTY_TO_DATA.get(property));
                continue;
            }

            if (PipeBlock.PROPERTY_BY_DIRECTION.containsValue(property)) {
                pipeProps++;
            }
        }

        if (!extensions.isEmpty()) {
            if (isExactly(extensions, Switch.class)) {
                return Switch.class;
            }
            if (isExactly(extensions, RedstoneRail.class)) {
                return RedstoneRail.class;
            }

            return extensions.iterator().next();
        }

        for (Property<?> property : data.complexPropertyFields().values()) {
            if (PipeBlock.PROPERTY_BY_DIRECTION.containsValue(property)) {
                pipeProps++;
            }
        }

        if (pipeProps >= 2) {
            return MultipleFacing.class;
        }
        return null;
    }

    private static boolean isExactly(Set<Class<? extends org.bukkit.block.data.BlockData>> extensions, Class<? extends org.bukkit.block.data.BlockData> globClass) {
        return extensions.equals(ClassHelper.getAllInterfaces(globClass, org.bukkit.block.data.BlockData.class, new HashSet<>()));
    }*/

    private static String formatApiName(Class<?> specialBlock) {
        String apiName = specialBlock.getSimpleName();
        if (!BLOCK_SUFFIX_INTENDED.contains(specialBlock)) {
            return apiName.substring(0, apiName.length() - "Block".length());
        }
        return apiName;
    }

    private static boolean handleComplexType(Field field, BiConsumer<Field, Property<?>> complexCallback) throws IllegalAccessException {
        if (field.getType().isArray() && Property.class.isAssignableFrom(field.getType().getComponentType())) {
            if (!field.trySetAccessible()) {
                return true;
            }

            for (Property<?> property : (Property<?>[]) field.get(null)) {
                complexCallback.accept(field, property);
            }
            return true;
        }
        if (Iterable.class.isAssignableFrom(field.getType()) && field.getGenericType() instanceof ParameterizedType complexType) {
            Type[] args = complexType.getActualTypeArguments();
            if (args.length == 1 && Property.class.isAssignableFrom(ClassHelper.eraseType(args[0]))) {
                if (!field.trySetAccessible()) {
                    return true;
                }

                for (Property<?> property : (Iterable<Property<?>>) field.get(null)) {
                    complexCallback.accept(field, property);
                }
            }
            return true;
        }
        if (Map.class.isAssignableFrom(field.getType()) && field.getGenericType() instanceof ParameterizedType complexType) {
            if (!field.trySetAccessible()) {
                return true;
            }

            Type[] args = complexType.getActualTypeArguments();
            if (args.length == 2 && Property.class.isAssignableFrom(ClassHelper.eraseType(args[1]))) {
                for (Property<?> property : ((Map<?, Property<?>>) field.get(null)).values()) {
                    complexCallback.accept(field, property);
                }
                return true;
            }
        }
        return false;
    }

    private static void fetchProperties(Class<?> block, BiConsumer<Field, Property<?>> simpleCallback, @Nullable BiConsumer<Field, Property<?>> complexCallback) {
        try {
            for (Field field : block.getDeclaredFields()) {
                if (ClassHelper.isStaticConstant(field, 0)) {
                    if (complexCallback != null && handleComplexType(field, complexCallback)) {
                        continue;
                    }

                    if (!Property.class.isAssignableFrom(field.getType())) {
                        continue;
                    }

                    if (field.trySetAccessible()) {
                        Property<?> property = ((Property<?>) field.get(null));
                        simpleCallback.accept(field, property);
                    }
                }
            }
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }

        if (block.isInterface()) {
            return;
        }

        // look deeper
        if (block.getSuperclass() != null && block.getSuperclass() != Block.class) {
            fetchProperties(block.getSuperclass(), simpleCallback, complexCallback);
        }
        for (Class<?> ext : block.getInterfaces()) {
            fetchProperties(ext, simpleCallback, complexCallback);
        }
    }
}
