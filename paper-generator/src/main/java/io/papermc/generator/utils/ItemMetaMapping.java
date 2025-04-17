package io.papermc.generator.utils;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.papermc.typewriter.ClassNamed;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Deprecated
public class ItemMetaMapping {

    public record BridgeData(ClassNamed api, String field) {

        public static final Codec<BridgeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
           SourceCodecs.CLASS_NAMED.fieldOf("api").forGetter(BridgeData::api),
           SourceCodecs.IDENTIFIER.fieldOf("field").forGetter(BridgeData::field)
        ).apply(instance, BridgeData::new));
    }

    public static final Map<ClassNamed, BridgeData> BRIDGE;
    public static final Codec<Map<ClassNamed, BridgeData>> BRIDGE_CODEC = Codec.unboundedMap(SourceCodecs.CLASS_NAMED, BridgeData.CODEC);
    static {
        try (Reader input = new BufferedReader(new InputStreamReader(ItemMetaMapping.class.getClassLoader().getResourceAsStream("data/item_meta/bridge.json")))) {
            JsonObject metas = SourceCodecs.GSON.fromJson(input, JsonObject.class);
            BRIDGE = Collections.unmodifiableMap(BRIDGE_CODEC.parse(JsonOps.INSTANCE, metas).getOrThrow());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static final Codec<ItemPredicate> PREDICATE_CODEC = ItemPredicate.Type.CODEC.dispatch("type", ItemPredicate::type, type -> type.codec);

    public interface ItemPredicate {

        Type type();

        enum Type implements StringRepresentable {
            INSTANCE_OF("instance_of", InstanceOfPredicate.CODEC),
            IS("is", IsPredicate.CODEC),
            ELEMENT("element", ElementPredicate.CODEC);

            public static final Codec<Type> CODEC = StringRepresentable.fromValues(Type::values);
            private final String name;
            final MapCodec<? extends ItemPredicate> codec;

            Type(final String name, final MapCodec<? extends ItemPredicate> codec) {
                this.name = name;
                this.codec = codec;
            }

            @Override
            public String getSerializedName() {
                return this.name;
            }
        }

        boolean test(Holder.Reference<Item> item);

        record InstanceOfPredicate(Class<?> value, boolean againstBlock) implements ItemPredicate {

            public static final MapCodec<InstanceOfPredicate> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                SourceCodecs.classCodec(Object.class).fieldOf("value").forGetter(InstanceOfPredicate::value),
                Codec.BOOL.optionalFieldOf("against_block", false).forGetter(InstanceOfPredicate::againstBlock)
            ).apply(instance, InstanceOfPredicate::new));

            @Override
            public Type type() {
                return Type.INSTANCE_OF;
            }

            @Override
            public boolean test(Holder.Reference<Item> item) {
                if (!this.againstBlock) {
                    return this.value.isAssignableFrom(item.value().getClass());
                }

                if (!(item.value() instanceof BlockItem blockItem)) {
                    return false;
                }

                return this.value.isAssignableFrom(blockItem.getBlock().getClass());
            }
        }

        record IsPredicate(Class<?> value, boolean againstBlock) implements ItemPredicate {

            public static final MapCodec<InstanceOfPredicate> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                SourceCodecs.classCodec(Object.class).fieldOf("value").forGetter(InstanceOfPredicate::value),
                Codec.BOOL.optionalFieldOf("against_block", false).forGetter(InstanceOfPredicate::againstBlock)
            ).apply(instance, InstanceOfPredicate::new));

            @Override
            public Type type() {
                return Type.IS;
            }

            @Override
            public boolean test(Holder.Reference<Item> item) {
                if (!this.againstBlock) {
                    return this.value.equals(item.value().getClass());
                }

                if (!(item.value() instanceof BlockItem blockItem)) {
                    return false;
                }

                return this.value.equals(blockItem.getBlock().getClass());
            }
        }

        record ElementPredicate(ExtraCodecs.TagOrElementLocation value) implements ItemPredicate {

            public static final MapCodec<ElementPredicate> DIRECT_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ExtraCodecs.TAG_OR_ELEMENT_ID.fieldOf("value").forGetter(ElementPredicate::value)
            ).apply(instance, ElementPredicate::new));

            public static final Codec<ElementPredicate> INLINED_CODEC = ExtraCodecs.TAG_OR_ELEMENT_ID.xmap(ElementPredicate::new, ElementPredicate::value);

            public static final MapCodec<ElementPredicate> CODEC = null/*Codec.withAlternative(INLINED_CODEC, DIRECT_CODEC)*/;

            @Override
            public Type type() {
                return Type.IS;
            }

            @Override
            public boolean test(Holder.Reference<Item> item) {
                if (this.value.tag()) {
                    return item.is(TagKey.create(Registries.ITEM, this.value.id()));
                }

                return item.is(this.value.id());
            }
        }
    }

    public static final Map<ClassNamed, List<ItemPredicate>> PREDICATES;
    public static final Codec<Map<ClassNamed, List<ItemPredicate>>> PREDICATE_MAP_CODEC = Codec.unboundedMap(SourceCodecs.CLASS_NAMED, PREDICATE_CODEC.listOf(1, Integer.MAX_VALUE));
    static {
        try (Reader input = new BufferedReader(new InputStreamReader(ItemMetaMapping.class.getClassLoader().getResourceAsStream("data/item_meta/predicates.json")))) {
            JsonObject predicates = SourceCodecs.GSON.fromJson(input, JsonObject.class);
            PREDICATES = Collections.unmodifiableMap(PREDICATE_MAP_CODEC.parse(JsonOps.INSTANCE, predicates).getOrThrow());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
