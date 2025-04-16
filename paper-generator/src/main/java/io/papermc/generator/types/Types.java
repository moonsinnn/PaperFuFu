package io.papermc.generator.types;

import com.squareup.javapoet.ClassName;
import io.papermc.typewriter.ClassNamed;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class Types {

    public static final String API_PACKAGE = "org.bukkit";
    public static final String PAPER_PACKAGE = "io.papermc.paper";

    public static final ClassName NAMESPACED_KEY = ClassName.get(API_PACKAGE, "NamespacedKey");

    public static final ClassName MINECRAFT_EXPERIMENTAL = ClassName.get(API_PACKAGE, "MinecraftExperimental");

    public static final ClassName MINECRAFT_EXPERIMENTAL_REQUIRES = ClassName.get(API_PACKAGE, "MinecraftExperimental", "Requires");

    public static final ClassName KEY = ClassName.get("net.kyori.adventure.key", "Key");

    public static final ClassName REGISTRY_KEY = ClassName.get(PAPER_PACKAGE + ".registry", "RegistryKey");

    public static final ClassName TYPED_KEY = ClassName.get(PAPER_PACKAGE + ".registry", "TypedKey");

    public static final ClassName GOAL_KEY = ClassName.get("com.destroystokyo.paper.entity.ai", "GoalKey");

    public static final ClassName GOAL = ClassName.get("com.destroystokyo.paper.entity.ai", "Goal");

    public static final ClassName MOB = ClassName.get(API_PACKAGE + ".entity", "Mob");

    public static final ClassName RANGED_ENTITY = ClassName.get("com.destroystokyo.paper.entity", "RangedEntity");

    public static final ClassName TAG_KEY = ClassName.get(PAPER_PACKAGE + ".registry.tag", "TagKey");

    public static final ClassName GENERATED_FROM = ClassName.get(PAPER_PACKAGE + ".generated", "GeneratedFrom");

    public static final String IMPL_PACKAGE = "org.bukkit.craftbukkit";

    public static final ClassName CRAFT_BLOCK_DATA = ClassName.get(IMPL_PACKAGE + ".block.data", "CraftBlockData");

    public static final ClassName CRAFT_BLOCK = ClassName.get(IMPL_PACKAGE + ".block", "CraftBlock");

    public static ClassName typed(ClassNamed name) {
        if (name.knownClass() != null) {
            return ClassName.get(name.knownClass());
        }

        return ClassName.bestGuess(name.canonicalName());
    }
}
