package io.papermc.generator.utils;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.squareup.javapoet.ClassName;
import io.papermc.generator.Main;
import io.papermc.generator.utils.predicate.BlockPredicate;
import io.papermc.typewriter.ClassNamed;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.StringRepresentable;

public final class BlockStateData {

    public static final Map<ClassNamed, List<BlockPredicate>> PREDICATES;
    public static final Codec<Map<ClassNamed, List<BlockPredicate>>> PREDICATES_CODEC = Codec.unboundedMap(
        SourceCodecs.CLASS_NAMED, BlockPredicate.CODEC.listOf()
    );

    static {
        try (Reader input = new BufferedReader(new InputStreamReader(BlockStateMapping.class.getClassLoader().getResourceAsStream("data/block_state/predicates.json")))) {
            JsonObject predicates = SourceCodecs.GSON.fromJson(input, JsonObject.class);
            PREDICATES = PREDICATES_CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, Main.REGISTRY_ACCESS), predicates).getOrThrow();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static final Map<Class<? extends Enum<? extends StringRepresentable>>, ClassName> ENUM_PROPERTY_TYPES;
    public static final Codec<Map<Class<? extends Enum<? extends StringRepresentable>>, ClassName>> ENUM_PROPERTY_TYPES_CODEC = Codec.unboundedMap(
        SourceCodecs.classCodec(new TypeToken<Enum<? extends StringRepresentable>>() {}), SourceCodecs.CLASS_NAME
    );

    static {
        try (Reader input = new BufferedReader(new InputStreamReader(BlockStateMapping.class.getClassLoader().getResourceAsStream("data/block_state/enum_property_types.json")))) {
            JsonObject properties = SourceCodecs.GSON.fromJson(input, JsonObject.class);
            ENUM_PROPERTY_TYPES = ENUM_PROPERTY_TYPES_CODEC.parse(JsonOps.INSTANCE, properties).getOrThrow();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private BlockStateData() {
    }
}
