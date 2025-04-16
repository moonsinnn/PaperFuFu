package io.papermc.generator.rewriter.types.registry;

import io.papermc.generator.registry.RegistryEntries;
import io.papermc.generator.rewriter.types.Types;
import io.papermc.typewriter.replace.SearchMetadata;
import io.papermc.typewriter.replace.SearchReplaceRewriter;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import java.util.Set;

public class RegistriesArgumentProviderRewriter extends SearchReplaceRewriter {

    private static final Set<ResourceKey<? extends Registry<?>>> NEW_CONVERT_METHODS = Set.of(
        Registries.ITEM,
        Registries.BLOCK
    );

    @Override
    public void insert(SearchMetadata metadata, StringBuilder builder) {
        RegistryEntries.forEach(entry -> {
            final String format;
            if (NEW_CONVERT_METHODS.contains(entry.getRegistryKey())) {
                format = "%s.%s, %s.class, %s.%s, %s.class, %s.class, true";
            } else {
                format = "%s.%s, %s.class, %s.%s, %s.class, %s.class";
            }

            builder.append(metadata.indent());
            builder.append("register(").append(format.formatted(
                Types.REGISTRY_KEY.simpleName(),
                entry.registryKeyField(),
                this.importCollector.getShortName(entry.data().api().klass()),
                Registries.class.getSimpleName(),
                entry.registryKeyField(),
                this.importCollector.getShortName(entry.data().impl().klass()),
                this.importCollector.getShortName(entry.elementClass())
            )).append(");");
            builder.append("\n");
        });
    }
}
