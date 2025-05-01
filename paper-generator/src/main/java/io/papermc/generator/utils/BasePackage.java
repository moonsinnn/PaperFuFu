package io.papermc.generator.utils;

import com.squareup.javapoet.ClassName;
import io.papermc.typewriter.ClassNamed;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record BasePackage(String api, String impl) {

    public static final BasePackage PAPER = new BasePackage("io.papermc.paper");
    public static final BasePackage BUKKIT = new BasePackage("org.bukkit", "org.bukkit.craftbukkit");
    @ApiStatus.Obsolete
    public static final BasePackage PAPER_LEGACY = new BasePackage("com.destroystokyo.paper");
    @Deprecated
    public static final BasePackage SPIGOT = new BasePackage("org.spigotmc");

    BasePackage(String name) {
        this(name, name);
    }

    public ClassName className(String simpleName, String... simpleNames) {
        return relativeClass(null, simpleName, simpleNames);
    }

    public ClassName relativeClass(@Nullable String packageName, String simpleName, String... simpleNames) {
        return ClassName.get(packageName == null ? this.api : String.join(".", this.api, packageName), simpleName, simpleNames);
    }

    public ClassName relativeImpClass(@Nullable String packageName, String simpleName, String... simpleNames) {
        return ClassName.get(packageName == null ? this.impl : String.join(".", this.impl, packageName), simpleName, simpleNames);
    }

    public ClassNamed classNamed(String simpleName, String... simpleNames) {
        return relativeClassName(null, simpleName, simpleNames);
    }

    public ClassNamed relativeClassName(@Nullable String packageName, String simpleName, String... simpleNames) {
        return ClassNamed.of(packageName == null ? this.api : String.join(".", this.api, packageName), simpleName, simpleNames);
    }

    public ClassNamed relativeImpClassName(@Nullable String packageName, String simpleName, String... simpleNames) {
        return ClassNamed.of(packageName == null ? this.impl : String.join(".", this.impl, packageName), simpleName, simpleNames);
    }
}
