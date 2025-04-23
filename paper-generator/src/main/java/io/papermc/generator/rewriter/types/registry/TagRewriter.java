package io.papermc.generator.rewriter.types.registry;

import io.papermc.generator.Main;
import io.papermc.generator.registry.RegistryEntries;
import io.papermc.generator.rewriter.types.Types;
import io.papermc.generator.rewriter.utils.Annotations;
import io.papermc.generator.utils.Formatting;
import io.papermc.generator.utils.experimental.SingleFlagHolder;
import io.papermc.typewriter.ClassNamed;
import io.papermc.typewriter.replace.SearchMetadata;
import io.papermc.typewriter.replace.SearchReplaceRewriter;
import java.util.Iterator;
import java.util.Locale;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.ApiStatus;

import static io.papermc.generator.utils.Formatting.quoted;

@ApiStatus.Obsolete
public class TagRewriter extends SearchReplaceRewriter {

    public record TagRegistry(String legacyFolderName, ClassNamed apiType, ResourceKey<? extends Registry<?>> registryKey) {
        public TagRegistry(String legacyFolderName, ResourceKey<? extends Registry<?>> registryKey) {
            this(legacyFolderName, RegistryEntries.BY_REGISTRY_KEY.get(registryKey).data().api().klass(), registryKey);
        }
    }

    private static final TagRegistry[] SUPPORTED_REGISTRIES = { // 1.21 folder name are normalized to registry key but api will stay as is
        new TagRegistry("blocks", Types.MATERIAL, Registries.BLOCK),
        new TagRegistry("items", Types.MATERIAL, Registries.ITEM),
        new TagRegistry("fluids", Registries.FLUID),
        new TagRegistry("entity_types", Registries.ENTITY_TYPE),
        new TagRegistry("game_events", Registries.GAME_EVENT)
        // new TagRegistry("damage_types", Registries.DAMAGE_TYPE) - separate in DamageTypeTags
    };

    @Override
    protected void insert(SearchMetadata metadata, StringBuilder builder) {
        for (int i = 0, len = SUPPORTED_REGISTRIES.length; i < len; i++) {
            final TagRegistry tagRegistry = SUPPORTED_REGISTRIES[i];

            final ResourceKey<? extends Registry<?>> registryKey = tagRegistry.registryKey();
            final Registry<?> registry = Main.REGISTRY_ACCESS.lookupOrThrow(registryKey);

            final String fieldPrefix = Formatting.formatTagFieldPrefix(tagRegistry.legacyFolderName(), registryKey);
            final String registryFieldName = "REGISTRY_" + tagRegistry.legacyFolderName().toUpperCase(Locale.ENGLISH);

            if (i != 0) {
                builder.append('\n'); // extra line before the registry field
            }

            // registry name field
            builder.append(metadata.indent());
            builder.append("%s %s = %s;".formatted(String.class.getSimpleName(), registryFieldName, quoted(tagRegistry.legacyFolderName())));

            builder.append('\n');
            builder.append('\n');

            Iterator<? extends TagKey<?>> keyIterator = registry.listTagIds().sorted(Formatting.alphabeticKeyOrder(tagKey -> tagKey.location().getPath())).iterator();

            while (keyIterator.hasNext()) {
                TagKey<?> tagKey = keyIterator.next();
                final String keyPath = tagKey.location().getPath();
                final String fieldName = fieldPrefix + Formatting.formatKeyAsField(keyPath);

                // tag field
                String featureFlagName = Main.EXPERIMENTAL_TAGS.get(tagKey);
                if (featureFlagName != null) {
                    Annotations.experimentalAnnotations(builder, metadata.indent(), this.importCollector, SingleFlagHolder.fromName(featureFlagName));
                }

                builder.append(metadata.indent());
                builder.append("%s<%s>".formatted(this.source.mainClass().simpleName(), this.importCollector.getShortName(tagRegistry.apiType()))).append(' ').append(fieldName);
                builder.append(" = ");
                builder.append("%s.getTag(%s, %s.minecraft(%s), %s.class)".formatted(Types.BUKKIT.simpleName(), registryFieldName, Types.NAMESPACED_KEY.simpleName(), quoted(keyPath), tagRegistry.apiType().simpleName())); // assume type is imported properly
                builder.append(';');

                builder.append('\n');
                if (keyIterator.hasNext()) {
                    builder.append('\n');
                }
            }
        }
    }
}
