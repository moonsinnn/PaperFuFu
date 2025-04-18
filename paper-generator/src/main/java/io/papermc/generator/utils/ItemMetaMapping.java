package io.papermc.generator.utils;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
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
            BRIDGE = BRIDGE_CODEC.parse(JsonOps.INSTANCE, metas).getOrThrow();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static final Codec<ItemPredicate> DIRECT_PREDICATE_CODEC = ItemPredicate.Type.CODEC.dispatch("type", ItemPredicate::type, type -> type.codec);
    public static final Codec<ItemPredicate> PREDICATE_CODEC = Codec.either(ItemPredicate.IsElementPredicate.COMPACT_CODEC, DIRECT_PREDICATE_CODEC).xmap(Either::unwrap, Either::right);

    public interface ItemPredicate {

        Type type();

        enum Type implements StringRepresentable {
            INSTANCE_OF("instance_of", InstanceOfPredicate.CODEC),
            IS_CLASS("is_class", IsClassPredicate.CODEC),
            IS_ELEMENT("is_element", IsElementPredicate.CODEC);

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

        record IsClassPredicate(Class<?> value, boolean againstBlock) implements ItemPredicate {

            public static final MapCodec<IsClassPredicate> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                SourceCodecs.CLASS.fieldOf("value").forGetter(IsClassPredicate::value),
                Codec.BOOL.optionalFieldOf("against_block", false).forGetter(IsClassPredicate::againstBlock)
            ).apply(instance, IsClassPredicate::new));

            @Override
            public Type type() {
                return Type.IS_CLASS;
            }

            @Override
            public boolean test(Holder.Reference<Item> item) {
                if (!this.againstBlock) {
                    return this.value.equals(item.value().getClass());
                } else if (item.value() instanceof BlockItem blockItem) {
                    return this.value.equals(blockItem.getBlock().getClass());
                }

                return false;
            }
        }

        record InstanceOfPredicate(Class<?> value, boolean againstBlock) implements ItemPredicate {

            public static final MapCodec<InstanceOfPredicate> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                SourceCodecs.CLASS.fieldOf("value").forGetter(InstanceOfPredicate::value),
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
                } else if (item.value() instanceof BlockItem blockItem) {
                    return this.value.isAssignableFrom(blockItem.getBlock().getClass());
                }

                return false;
            }
        }

        record IsElementPredicate(ExtraCodecs.TagOrElementLocation value) implements ItemPredicate {

            public static final MapCodec<IsElementPredicate> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ExtraCodecs.TAG_OR_ELEMENT_ID.fieldOf("value").forGetter(IsElementPredicate::value)
            ).apply(instance, IsElementPredicate::new));

            public static final Codec<IsElementPredicate> COMPACT_CODEC = ExtraCodecs.TAG_OR_ELEMENT_ID.xmap(IsElementPredicate::new, IsElementPredicate::value);

            @Override
            public Type type() {
                return Type.IS_ELEMENT;
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
    public static final Codec<Map<ClassNamed, List<ItemPredicate>>> PREDICATES_CODEC = Codec.unboundedMap(SourceCodecs.CLASS_NAMED, PREDICATE_CODEC.listOf(1, Integer.MAX_VALUE));
    static {
        try (Reader input = new BufferedReader(new InputStreamReader(ItemMetaMapping.class.getClassLoader().getResourceAsStream("data/item_meta/predicates.json")))) {
            JsonObject predicates = SourceCodecs.GSON.fromJson(input, JsonObject.class);
            PREDICATES = PREDICATES_CODEC.parse(JsonOps.INSTANCE, predicates).getOrThrow();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
