package io.papermc.generator.utils;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import io.papermc.generator.Main;
import io.papermc.generator.rewriter.types.simple.EntityTypeRewriter;
import io.papermc.generator.types.goal.MobGoalNames;
import io.papermc.typewriter.ClassNamed;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PrepareInputFiles {

    static {
        Main.bootStrap(true);
    }

    @FunctionalInterface
    public interface Transmuter<A> {

        A transmute(A value);
    }

    private record DataFile<A>(String path, Codec<A> codec, Transmuter<A> transmuter) {

        public A transmuteRaw(Object value) {
            return this.transmuter.transmute((A) value);
        }
    }

    private static <K, V> Transmuter<Map<K, V>> transmuteMap(Comparator<K> comparator) {
        return value -> {
            TreeMap<K, V> map = new TreeMap<>(comparator);
            map.putAll(value);
            return map;
        };
    }

    private static final List<DataFile<?>> DATA_FILES = List.of(
        new DataFile<>("block_state_properties.json", BlockStateMapping.PROPERTY_DATA_CODEC, value -> {
            List<BlockStateMapping.PropertyData> mutableValue = new ArrayList<>(value);
            mutableValue.sort(Comparator.naturalOrder());
            return mutableValue;
        }),
        new DataFile<>("enum_property_types.json", BlockStateMapping.ENUM_PROPERTY_TYPES_CODEC, transmuteMap(Comparator.comparing((Class<?> klass) -> klass.getCanonicalName()))),
        new DataFile<>("entity_types.json", EntityTypeRewriter.DATA_CODEC, transmuteMap(Formatting.alphabeticKeyOrder((ResourceKey<EntityType<?>> key) -> key.location().getPath()))),
        new DataFile<>("entity_class_names.json", MobGoalNames.ENTITY_CLASS_NAMES_CODEC, transmuteMap(Comparator.comparing((Class<?> klass) -> klass.getCanonicalName()))),
        new DataFile<>("item_meta/bridge.json", ItemMetaData.BRIDGE_CODEC, transmuteMap(Comparator.comparing(ClassNamed::canonicalName)))
    );

    public static void main(String[] args) throws IOException {
        String resourceDir = args[0];
        for (DataFile<?> file : DATA_FILES) {
            Path resourcePath = Path.of(resourceDir, "data/" + file.path());
            try (Reader input = Files.newBufferedReader(resourcePath)) {
                JsonElement element = SourceCodecs.GSON.fromJson(input, JsonElement.class);
                Object javaElement = file.transmuteRaw(file.codec().parse(JsonOps.INSTANCE, element).getOrThrow());
                element = ((Codec<Object>) file.codec()).encodeStart(JsonOps.INSTANCE, javaElement).getOrThrow();
                Files.writeString(resourcePath, SourceCodecs.GSON.toJson(element) + "\n", StandardCharsets.UTF_8);
            } catch (IOException ex) {
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }
        }
    }
}
