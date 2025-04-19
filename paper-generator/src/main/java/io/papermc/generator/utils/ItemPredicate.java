package io.papermc.generator.utils;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.papermc.generator.Main;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public sealed interface ItemPredicate permits ItemPredicate.IsClassPredicate, ItemPredicate.InstanceOfPredicate, ItemPredicate.IsElementPredicate {

    Codec<ItemPredicate> DIRECT_CODEC = Type.CODEC.dispatch("type", ItemPredicate::type, type -> type.codec);
    Codec<ItemPredicate> CODEC = Codec.either(IsElementPredicate.COMPACT_CODEC, DIRECT_CODEC).xmap(Either::unwrap, Either::right);

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

        private static final Codec<ExtraCodecs.TagOrElementLocation> ITEM_TAG_OR_ELEMENT_ID = ExtraCodecs.TAG_OR_ELEMENT_ID.validate(
            value -> {
                if (value.tag()) {
                    if (BuiltInRegistries.ITEM.get(TagKey.create(Registries.ITEM, value.id())).isPresent()) {
                        return DataResult.success(value);
                    } else {
                        return DataResult.error(() -> "Invalid tag id: " + value);
                    }
                } else {
                    if (BuiltInRegistries.ITEM.get(value.id()).isPresent()) {
                        return DataResult.success(value);
                    } else {
                        return DataResult.error(() -> "Invalid element id: " + value);
                    }
                }
            }
        );

        public static final MapCodec<IsElementPredicate> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ITEM_TAG_OR_ELEMENT_ID.fieldOf("value").forGetter(IsElementPredicate::value)
        ).apply(instance, IsElementPredicate::new));

        public static final Codec<IsElementPredicate> COMPACT_CODEC = ITEM_TAG_OR_ELEMENT_ID.xmap(IsElementPredicate::new, IsElementPredicate::value);

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
