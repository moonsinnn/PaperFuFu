package io.papermc.generator.utils.predicate;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.papermc.generator.utils.BlockStateMapping;
import io.papermc.generator.utils.SourceCodecs;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

@NullMarked
public sealed interface BlockPropertyPredicate permits BlockPropertyPredicate.IsNamePredicate, BlockPropertyPredicate.IsFieldPredicate {

    Codec<BlockPropertyPredicate> DIRECT_CODEC = Type.CODEC.dispatch("type", BlockPropertyPredicate::type, type -> type.codec);
    Codec<BlockPropertyPredicate> CODEC = Codec.either(BlockPropertyPredicate.IsNamePredicate.COMPACT_CODEC, DIRECT_CODEC).xmap(Either::unwrap, Either::right);

    Codec<Set<BlockPropertyPredicate>> SET_CODEC = CODEC.listOf().xmap(Set::copyOf, List::copyOf);
    Codec<Set<BlockPropertyPredicate>> NON_EMPTY_SET_CODEC = ExtraCodecs.nonEmptyList(CODEC.listOf()).xmap(Set::copyOf, List::copyOf);

    Type type();

    enum Type implements StringRepresentable {
        IS_FIELD("is_field", IsFieldPredicate.CODEC),
        IS_NAME("is_name", IsNamePredicate.CODEC);

        public static final Codec<Type> CODEC = StringRepresentable.fromValues(Type::values);
        private final String name;
        final MapCodec<? extends BlockPropertyPredicate> codec;

        Type(final String name, final MapCodec<? extends BlockPropertyPredicate> codec) {
            this.name = name;
            this.codec = codec;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    boolean test(Property<?> property);

    static boolean testProperty(Predicate<String> predicate, String id, boolean supportInversion) {
        if (supportInversion && !id.isEmpty() && id.charAt(0) == '!') {
            return !predicate.test(id.substring(1));
        }

        return predicate.test(id);
    }

    record IsNamePredicate(String value) implements BlockPropertyPredicate {

        public static final MapCodec<IsNamePredicate> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("value").forGetter(IsNamePredicate::value)
        ).apply(instance, IsNamePredicate::new));

        public static final Codec<IsNamePredicate> COMPACT_CODEC = Codec.STRING.xmap(IsNamePredicate::new, IsNamePredicate::value);

        @Override
        public Type type() {
            return Type.IS_NAME;
        }

        @Override
        public boolean test(Property<?> property) {
            return BlockPropertyPredicate.testProperty(
                name -> name.equals(property.getName()),
                this.value,
                true
            );
        }
    }

    record IsFieldPredicate(String value) implements BlockPropertyPredicate {

        public static final MapCodec<IsFieldPredicate> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SourceCodecs.IDENTIFIER.fieldOf("value").forGetter(IsFieldPredicate::value)
        ).apply(instance, IsFieldPredicate::new));

        @Override
        public Type type() {
            return Type.IS_FIELD;
        }

        @Override
        public boolean test(Property<?> property) {
            return BlockPropertyPredicate.testProperty(
                field -> field.equals(BlockStateMapping.GENERIC_FIELDS.get(property).getName()),
                this.value,
                true
            );
        }
    }
}
