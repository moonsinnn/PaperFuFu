package io.papermc.generator.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.squareup.javapoet.ClassName;
import io.papermc.typewriter.ClassNamed;
import javax.lang.model.SourceVersion;

public final class SourceCodecs {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private SourceCodecs() {
    }

    public static final Codec<String> IDENTIFIER = Codec.STRING.validate(name -> {
        return SourceVersion.isIdentifier(name) && !SourceVersion.isKeyword(name) ? DataResult.success(name) : DataResult.error(() -> "Invalid identifier: %s".formatted(name));
    });

    public static final Codec<String> BINARY_CLASS_NAME = Codec.STRING.validate(name -> {
        return SourceVersion.isName(name.replace('$', '.')) ? DataResult.success(name) : DataResult.error(() -> "Invalid binary name: %s".formatted(name));
    });

    public static final Codec<ClassNamed> CLASS_NAMED = BINARY_CLASS_NAME.xmap(name -> {
        int lastDotIndex = name.lastIndexOf('.');
        if (lastDotIndex != -1) {
            return ClassNamed.of(name.substring(0, lastDotIndex), name.substring(lastDotIndex + 1));
        }

        return ClassNamed.of("", name);
    }, ClassNamed::binaryName);

    public static final Codec<ClassName> CLASS_NAME = CLASS_NAMED.xmap(
        io.papermc.generator.types.Types::typed, io.papermc.generator.rewriter.types.Types::typed
    );
}
