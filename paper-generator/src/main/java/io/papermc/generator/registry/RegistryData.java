package io.papermc.generator.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.lang.constant.ConstantDescs;
import java.util.Optional;
import io.papermc.generator.utils.SourceCodecs;
import io.papermc.typewriter.ClassNamed;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record RegistryData(
    Api api,
    Impl impl,
    Optional<Builder> builder,
    boolean keyClassNameBasedOnApi,
    Optional<String> serializationUpdaterField,
    boolean delayed,
    boolean allowInline
) {

    public static final Codec<RegistryData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Api.CODEC.fieldOf("api").forGetter(RegistryData::api),
        Impl.CODEC.fieldOf("impl").forGetter(RegistryData::impl),
        Builder.CODEC.optionalFieldOf("builder").forGetter(RegistryData::builder),
        Codec.BOOL.optionalFieldOf("key_class_name_based_on_api", false).forGetter(RegistryData::keyClassNameBasedOnApi),
        SourceCodecs.IDENTIFIER.optionalFieldOf("serialization_updater_field").forGetter(RegistryData::serializationUpdaterField),
        Codec.BOOL.optionalFieldOf("delayed", false).deprecated(21).forGetter(RegistryData::delayed),
        Codec.BOOL.optionalFieldOf("allow_inline", false).forGetter(RegistryData::allowInline)
    ).apply(instance, RegistryData::new));

    public record Api(ClassNamed klass, Optional<ClassNamed> holders, boolean legacyEnum, Optional<String> registryField) {
        public Api(ClassNamed klass) {
            this(klass, Optional.of(klass), false, Optional.empty());
        }

        public static final Codec<Api> DIRECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SourceCodecs.CLASS_NAMED.fieldOf("class").forGetter(Api::klass),
            SourceCodecs.CLASS_NAMED.optionalFieldOf("holders").forGetter(Api::holders),
            Codec.BOOL.optionalFieldOf("legacy_enum", false).deprecated(8).forGetter(Api::legacyEnum),
            SourceCodecs.IDENTIFIER.optionalFieldOf("registry_field").forGetter(Api::registryField)
        ).apply(instance, Api::new));

        public static final Codec<Api> CLASS_ONLY_CODEC = SourceCodecs.CLASS_NAMED.xmap(Api::new, Api::klass);

        public static final Codec<Api> CODEC = Codec.withAlternative(CLASS_ONLY_CODEC, DIRECT_CODEC);
    }

    public record Impl(ClassNamed klass, String instanceMethod) {
        public Impl(ClassNamed klass) {
            this(klass, ConstantDescs.INIT_NAME);
        }

        public static final Codec<Impl> DIRECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SourceCodecs.CLASS_NAMED.fieldOf("class").forGetter(Impl::klass),
            SourceCodecs.IDENTIFIER.optionalFieldOf("instance_method", ConstantDescs.INIT_NAME).forGetter(Impl::instanceMethod)
        ).apply(instance, Impl::new));

        public static final Codec<Impl> CLASS_ONLY_CODEC = SourceCodecs.CLASS_NAMED.xmap(Impl::new, Impl::klass);

        public static final Codec<Impl> CODEC = Codec.withAlternative(CLASS_ONLY_CODEC, DIRECT_CODEC);
    }

    public record Builder(ClassNamed api, ClassNamed impl) {

        public static final Codec<Builder> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SourceCodecs.CLASS_NAMED.fieldOf("api").forGetter(Builder::api),
            SourceCodecs.CLASS_NAMED.fieldOf("impl").forGetter(Builder::impl)
        ).apply(instance, Builder::new));
    }
}
