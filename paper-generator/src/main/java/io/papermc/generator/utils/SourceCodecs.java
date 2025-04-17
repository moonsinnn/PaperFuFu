package io.papermc.generator.utils;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.squareup.javapoet.ClassName;
import io.papermc.typewriter.ClassNamed;
import javax.lang.model.SourceVersion;
import org.checkerframework.checker.units.qual.C;

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

    public static <T> Codec<Class<? extends T>> classCodec(Class<T> baseClass) { // todo or-class
        return BINARY_CLASS_NAME.comapFlatMap(name -> {
            try {
                Class<?> klass = Class.forName(name);
                if (baseClass.isAssignableFrom(klass)) {
                    return DataResult.success((Class<? extends T>) klass);
                }
                return DataResult.error(() -> "Missmatch class type: %s doesn't extends %s".formatted(klass, baseClass));
            } catch (ClassNotFoundException e) {
                return DataResult.error(e::getMessage);
            }
        }, Class::toString);
    }

    public static <T> Codec<Class<? extends T>> classCodec(TypeToken<T> baseClass) {
        return BINARY_CLASS_NAME.comapFlatMap(name -> {
            try {
                Class<?> klass = Class.forName(name);
                if (baseClass.isSupertypeOf(klass)) {
                    return DataResult.success((Class<? extends T>) klass);
                }
                return DataResult.error(() -> "Missmatch class type: %s doesn't extends %s".formatted(klass, baseClass));
            } catch (ClassNotFoundException e) {
                return DataResult.error(e::getMessage);
            }
        }, Class::toString);
    }

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
