package io.papermc.generator.rewriter.types.simple;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.papermc.generator.rewriter.types.registry.EnumRegistryRewriter;
import io.papermc.generator.utils.SourceCodecs;
import io.papermc.typewriter.ClassNamed;
import io.papermc.typewriter.preset.model.EnumValue;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.EntityType;

import static io.papermc.generator.utils.Formatting.quoted;

public class EntityTypeRewriter extends EnumRegistryRewriter<EntityType<?>> {

    private static final Gson GSON = new Gson();

    record Data(ClassNamed api, int legacyId) {

        public Data(ClassNamed api) {
            this(api, 0);
        }

        public static final Codec<Data> DIRECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SourceCodecs.CLASS_NAMED.fieldOf("api").forGetter(Data::api),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("legacy_id", 0).deprecated(13).forGetter(Data::legacyId)
        ).apply(instance, Data::new));

        private static final Codec<Data> CLASS_ONLY_CODEC = SourceCodecs.CLASS_NAMED.xmap(Data::new, Data::api);

        public static final Codec<Data> CODEC = Codec.withAlternative(CLASS_ONLY_CODEC, DIRECT_CODEC);
    }

    private static final Map<ResourceKey<? extends EntityType<?>>, Data> DATA;
    static {
        Map<ResourceKey<? extends EntityType<?>>, Data> data = new IdentityHashMap<>();
        try (Reader input = new BufferedReader(new InputStreamReader(EntityTypeRewriter.class.getClassLoader().getResourceAsStream("data/entity_types.json")))) {
            JsonObject registries = GSON.fromJson(input, JsonObject.class);
            for (String rawKey : registries.keySet()) {
                ResourceKey<? extends EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(rawKey));
                data.put(key, Data.CODEC.parse(JsonOps.INSTANCE, registries.get(rawKey)).getOrThrow());
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        DATA = Collections.unmodifiableMap(data);
    }

    public EntityTypeRewriter() {
        super(Registries.ENTITY_TYPE, false);
    }

    @Override
    protected EnumValue.Builder rewriteEnumValue(Holder.Reference<EntityType<?>> reference) {
        Data data = DATA.get(reference.key());
        String path = reference.key().location().getPath();
        List<String> arguments = new ArrayList<>(4);
        arguments.add(quoted(path));
        arguments.add(this.importCollector.getShortName(data.api()).concat(".class"));
        arguments.add(Integer.toString(data.legacyId()));

        if (!reference.value().canSummon()) {
            arguments.add(Boolean.FALSE.toString());
        }
        return super.rewriteEnumValue(reference).arguments(arguments);
    }
}
