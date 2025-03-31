package io.papermc.generator.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.squareup.javapoet.ClassName;
import io.papermc.typewriter.ClassNamed;
import javax.lang.model.SourceVersion;

public final class SourceCodecs {

    private SourceCodecs() {
    }

    public static final Codec<String> IDENTIFIER = Codec.STRING.validate(name -> {
        return SourceVersion.isIdentifier(name) && !SourceVersion.isKeyword(name) ? DataResult.success(name) : DataResult.error(() -> "Invalid identifier: %s".formatted(name));
    });

    public static final Codec<String> CLASS_NAME = Codec.STRING.validate(name -> {
        return SourceVersion.isName(name.replace('$', '.')) ? DataResult.success(name) : DataResult.error(() -> "Invalid class name: %s".formatted(name));
    });

    public static final Codec<ClassNamed> CLASS_NAMED = CLASS_NAME.xmap(name -> {
        int lastDotIndex = name.lastIndexOf('.');
        if (lastDotIndex != -1) {
            return ClassNamed.of(name.substring(0, lastDotIndex), name.substring(lastDotIndex + 1));
        }

        return ClassNamed.of("", name);
    }, ClassNamed::binaryName);

    public static final Codec<ClassName> CLASS_NAMED_JAVAPOET = CLASS_NAMED.xmap(
        io.papermc.generator.types.Types::typed, io.papermc.generator.rewriter.types.Types::typed
    );
}
