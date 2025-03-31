package io.papermc.generator.rewriter.types.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public interface RegistryIdentifiable<T> {

    ResourceKey<? extends Registry<T>> getRegistryKey();
}
