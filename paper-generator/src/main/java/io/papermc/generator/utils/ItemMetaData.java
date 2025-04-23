package io.papermc.generator.utils;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.papermc.generator.Main;
import io.papermc.generator.utils.predicate.ItemPredicate;
import io.papermc.typewriter.ClassNamed;
import net.minecraft.resources.RegistryOps;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;

@Deprecated
public final class ItemMetaData {

    public record BridgeData(ClassNamed api, String field) {

        public static final Codec<BridgeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
           SourceCodecs.CLASS_NAMED.fieldOf("api").forGetter(BridgeData::api),
           SourceCodecs.IDENTIFIER.fieldOf("field").forGetter(BridgeData::field)
        ).apply(instance, BridgeData::new));
    }

    public static final Map<ClassNamed, BridgeData> BRIDGE;
    public static final Codec<Map<ClassNamed, BridgeData>> BRIDGE_CODEC = Codec.unboundedMap(SourceCodecs.CLASS_NAMED, BridgeData.CODEC);
    static {
        try (Reader input = new BufferedReader(new InputStreamReader(ItemMetaData.class.getClassLoader().getResourceAsStream("data/item_meta/bridge.json")))) {
            JsonObject metas = SourceCodecs.GSON.fromJson(input, JsonObject.class);
            BRIDGE = BRIDGE_CODEC.parse(JsonOps.INSTANCE, metas).getOrThrow();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static final Map<ClassNamed, List<ItemPredicate>> PREDICATES;
    public static final Codec<Map<ClassNamed, List<ItemPredicate>>> PREDICATES_CODEC = Codec.unboundedMap(SourceCodecs.CLASS_NAMED, ItemPredicate.CODEC.listOf(1, Integer.MAX_VALUE));
    static {
        try (Reader input = new BufferedReader(new InputStreamReader(ItemMetaData.class.getClassLoader().getResourceAsStream("data/item_meta/predicates.json")))) {
            JsonObject predicates = SourceCodecs.GSON.fromJson(input, JsonObject.class);
            PREDICATES = PREDICATES_CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, Main.REGISTRY_ACCESS), predicates).getOrThrow();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private ItemMetaData() {
    }
}
