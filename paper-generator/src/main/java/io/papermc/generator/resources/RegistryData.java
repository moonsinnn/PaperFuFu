package io.papermc.generator.resources;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.lang.constant.ConstantDescs;
import java.util.Optional;
import io.papermc.generator.utils.SourceCodecs;
import io.papermc.typewriter.ClassNamed;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record RegistryData(
    Api api,
    Impl impl,
    Optional<Builder> builder,
    Optional<String> serializationUpdaterField,
    boolean allowInline
) {

    public static final Codec<RegistryData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Api.CODEC.fieldOf("api").forGetter(RegistryData::api),
        Impl.CODEC.fieldOf("impl").forGetter(RegistryData::impl),
        Builder.CODEC.optionalFieldOf("builder").forGetter(RegistryData::builder),
        SourceCodecs.IDENTIFIER.optionalFieldOf("serialization_updater_field").forGetter(RegistryData::serializationUpdaterField),
        Codec.BOOL.optionalFieldOf("allow_inline", false).forGetter(RegistryData::allowInline)
    ).apply(instance, RegistryData::new));

    public record Api(ClassNamed klass, Optional<ClassNamed> holders, Type type, boolean keyClassNameRelate, Optional<String> registryField) {
        public Api(ClassNamed klass) {
            this(klass, Optional.of(klass), Type.INTERFACE, false, Optional.empty());
        }

        public static final Codec<Api> DIRECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SourceCodecs.CLASS_NAMED.fieldOf("class").forGetter(Api::klass),
            SourceCodecs.CLASS_NAMED.optionalFieldOf("holders").forGetter(Api::holders),
            Type.CODEC.optionalFieldOf("type", Type.INTERFACE).forGetter(Api::type),
            Codec.BOOL.optionalFieldOf("key_class_name_relate", false).forGetter(Api::keyClassNameRelate),
            SourceCodecs.IDENTIFIER.optionalFieldOf("registry_field").forGetter(Api::registryField)
        ).apply(instance, Api::new));

        public static final Codec<Api> CLASS_ONLY_CODEC = SourceCodecs.CLASS_NAMED.xmap(Api::new, Api::klass);

        public static final Codec<Api> CODEC = Codec.either(CLASS_ONLY_CODEC, DIRECT_CODEC).xmap(Either::unwrap, api -> {
            if ((api.holders().isEmpty() || api.klass().equals(api.holders().get())) &&
                api.type() == Type.INTERFACE && !api.keyClassNameRelate() && api.registryField().isEmpty()) {
                return Either.left(api);
            }
            return Either.right(api);
        });

        public enum Type implements StringRepresentable {
            INTERFACE("interface"),
            CLASS("class"),
            @Deprecated(since = "1.8")
            ENUM("enum");

            private final String name;
            static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

            Type(String name) {
                this.name = name;
            }

            @Override
            public String getSerializedName() {
                return this.name;
            }
        }
    }

    public record Impl(ClassNamed klass, String instanceMethod, boolean delayed) {
        public Impl(ClassNamed klass) {
            this(klass, ConstantDescs.INIT_NAME, false);
        }

        public static final Codec<Impl> DIRECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SourceCodecs.CLASS_NAMED.fieldOf("class").forGetter(Impl::klass),
            SourceCodecs.IDENTIFIER.optionalFieldOf("instance_method", ConstantDescs.INIT_NAME).forGetter(Impl::instanceMethod),
            Codec.BOOL.optionalFieldOf("delayed", false).deprecated(21).forGetter(Impl::delayed)
        ).apply(instance, Impl::new));

        public static final Codec<Impl> CLASS_ONLY_CODEC = SourceCodecs.CLASS_NAMED.xmap(Impl::new, Impl::klass);

        public static final Codec<Impl> CODEC = Codec.either(CLASS_ONLY_CODEC, DIRECT_CODEC).xmap(Either::unwrap, impl -> {
            if (impl.instanceMethod().equals(ConstantDescs.INIT_NAME) && !impl.delayed()) {
                return Either.left(impl);
            }
            return Either.right(impl);
        });
    }

    public record Builder(ClassNamed api, ClassNamed impl, RegisterCapability capability) {

        public static final Codec<Builder> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SourceCodecs.CLASS_NAMED.fieldOf("api").forGetter(Builder::api),
            SourceCodecs.CLASS_NAMED.fieldOf("impl").forGetter(Builder::impl),
            RegisterCapability.CODEC.optionalFieldOf("capability", RegisterCapability.WRITABLE).forGetter(Builder::capability)
        ).apply(instance, Builder::new));

        public enum RegisterCapability implements StringRepresentable {
            NONE("none"),
            ADDABLE("addable"),
            MODIFIABLE("modifiable"),
            WRITABLE("writable");

            private final String name;
            static final Codec<RegisterCapability> CODEC = StringRepresentable.fromEnum(RegisterCapability::values);

            RegisterCapability(String name) {
                this.name = name;
            }

            @Override
            public String getSerializedName() {
                return this.name;
            }
        }
    }
}
