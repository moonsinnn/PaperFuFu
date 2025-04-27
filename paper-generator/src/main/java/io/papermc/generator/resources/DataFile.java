package io.papermc.generator.resources;

import com.google.gson.FormattingStyle;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import io.papermc.generator.Main;
import io.papermc.generator.types.SimpleGenerator;
import net.minecraft.resources.RegistryOps;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.NullMarked;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@NullMarked
public abstract class DataFile<V, A, R> {

    public static final Gson GSON = new GsonBuilder().setFormattingStyle(FormattingStyle.PRETTY.withIndent(SimpleGenerator.INDENT_UNIT)).create();

    private final String path;
    private final Codec<V> codec;
    private final Transmuter<V, A, R> transmuter;
    private final boolean requireRegistry;
    private @MonotonicNonNull V cached;

    protected DataFile(String path, Codec<V> codec, Transmuter<V, A, R> transmuter, boolean requireRegistry) {
        this.path = path;
        this.codec = codec;
        this.transmuter = transmuter;
        this.requireRegistry = requireRegistry;
    }

    protected DataFile(String path, Codec<V> codec, Transmuter<V, A, R> transmuter) {
        this(path, codec, transmuter, false);
    }

    public static class Map<K, V> extends DataFile<java.util.Map<K, V>, java.util.Map.Entry<K, V>, K> {
        public Map(String path, Codec<java.util.Map<K, V>> codec, Transmuter<java.util.Map<K, V>, java.util.Map.Entry<K, V>, K> transmuter, boolean requireRegistry) {
            super(path, codec, transmuter, requireRegistry);
        }

        public Map(String path, Codec<java.util.Map<K, V>> codec, Transmuter<java.util.Map<K, V>, java.util.Map.Entry<K, V>, K> transmuter) {
            super(path, codec, transmuter);
        }
    }

    public static class List<E> extends DataFile<java.util.List<E>, E, E> {
        public List(String path, Codec<java.util.List<E>> codec, Transmuter<java.util.List<E>, E, E> transmuter, boolean requireRegistry) {
            super(path, codec, transmuter, requireRegistry);
        }

        public List(String path, Codec<java.util.List<E>> codec, Transmuter<java.util.List<E>, E, E> transmuter) {
            super(path, codec, transmuter);
        }
    }

    public boolean isMutable() {
        return this.transmuter instanceof Transmuter.Mutable<V, A, R>;
    }

    @VisibleForTesting
    public Transmuter<V, A, R> transmuter() {
        return this.transmuter;
    }

    public V get() {
        if (this.cached == null) {
            this.cached = this.readUnchecked();
        }
        return this.cached;
    }

    public V readUnchecked() {
        try {
            return this.read();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public V read() throws IOException {
        try (Reader input = new BufferedReader(new InputStreamReader(DataFile.class.getClassLoader().getResourceAsStream(this.path)))) {
            JsonElement predicates = GSON.fromJson(input, JsonElement.class);
            return this.codec.parse(this.readOps(), predicates).getOrThrow();
        }
    }

    public MutationResult<V, A, R>  upgrade(Path destination) throws IOException {
        V value = this.read();
        MutationResult<V, A, R> result = this.transmuter.transmute(value);
        if (this.transmuter instanceof Transmuter.Mutable<V, A, R> mutableTransmuter) {
            value = mutableTransmuter.applyChanges(result);
        } else {
            value = result.value(); // sorted values
        }

        Files.writeString(destination, this.toJsonString(value), StandardCharsets.UTF_8);
        return result;
    }

    public String toJsonString(V object) {
        JsonElement element = this.codec.encodeStart(this.writeOps(), object).getOrThrow();
        return GSON.toJson(element) + "\n";
    }

    public String path() {
        return this.path;
    }

    private DynamicOps<JsonElement> readOps() {
        DynamicOps<JsonElement> ops = JsonOps.INSTANCE;
        if (this.requireRegistry) {
            ops = RegistryOps.create(ops, Main.REGISTRY_ACCESS);
        }
        return ops;
    }

    private DynamicOps<JsonElement> writeOps() {
        return JsonOps.INSTANCE;
    }

    @Override
    public String toString() {
        return "DataFile[path=" + this.path + ']';
    }
}
