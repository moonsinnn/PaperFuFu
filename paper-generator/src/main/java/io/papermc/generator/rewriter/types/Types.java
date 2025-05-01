package io.papermc.generator.rewriter.types;

import com.squareup.javapoet.ClassName;
import io.papermc.typewriter.ClassNamed;
import org.jspecify.annotations.NullMarked;
import java.util.ArrayList;
import java.util.List;

import static io.papermc.generator.utils.BasePackage.BUKKIT;
import static io.papermc.generator.utils.BasePackage.PAPER;
import static io.papermc.generator.utils.BasePackage.PAPER_LEGACY;

@NullMarked
public final class Types {

    public static final ClassNamed BUKKIT_CLASS = BUKKIT.classNamed("Bukkit");

    public static final ClassNamed FEATURE_FLAG = BUKKIT.classNamed("FeatureFlag");

    public static final ClassNamed REGISTRY = BUKKIT.classNamed("Registry");

    public static final ClassNamed TAG = BUKKIT.classNamed("Tag");

    public static final ClassNamed NAMESPACED_KEY = typed(io.papermc.generator.types.Types.NAMESPACED_KEY);

    public static final ClassNamed MINECRAFT_EXPERIMENTAL = typed(io.papermc.generator.types.Types.MINECRAFT_EXPERIMENTAL);
    public static final ClassNamed MINECRAFT_EXPERIMENTAL_REQUIRES = typed(io.papermc.generator.types.Types.MINECRAFT_EXPERIMENTAL_REQUIRES);

    @Deprecated
    public static final ClassNamed STATISTIC = BUKKIT.classNamed("Statistic");

    public static final ClassNamed STATISTIC_TYPE = BUKKIT.classNamed("Statistic", "Type");

    public static final ClassNamed BLOCK_TYPE_TYPED = BUKKIT.relativeClassName("block", "BlockType", "Typed");

    public static final ClassNamed ITEM_TYPE_TYPED = BUKKIT.relativeClassName("inventory", "ItemType", "Typed");

    public static final ClassNamed ITEM_META = BUKKIT.relativeClassName("inventory.meta", "ItemMeta");

    public static final ClassNamed LOCATION = BUKKIT.classNamed("Location");

    public static final ClassNamed MATERIAL = BUKKIT.classNamed("Material");

    public static final ClassNamed POSE = BUKKIT.relativeClassName("entity", "Pose");

    @Deprecated
    public static final ClassNamed VILLAGER = BUKKIT.relativeClassName("entity", "Villager");

    public static final ClassNamed SNIFFER_STATE = BUKKIT.relativeClassName("entity", "Sniffer", "State");

    public static final ClassNamed TROPICAL_FISH_PATTERN = BUKKIT.relativeClassName("entity", "TropicalFish", "Pattern");

    public static final ClassNamed FOX_TYPE = BUKKIT.relativeClassName("entity", "Fox", "Type");

    public static final ClassNamed SALMON_VARIANT = BUKKIT.relativeClassName("entity", "Salmon", "Variant");

    public static final ClassNamed PANDA_GENE = BUKKIT.relativeClassName("entity", "Panda", "Gene");

    public static final ClassNamed BOAT_STATUS = BUKKIT.relativeClassName("entity", "Boat", "Status");

    public static final ClassNamed ITEM_RARITY = BUKKIT.relativeClassName("inventory", "ItemRarity");

    public static final ClassNamed COOKING_BOOK_CATEGORY = BUKKIT.relativeClassName("inventory.recipe", "CookingBookCategory");

    public static final ClassNamed CRAFTING_BOOK_CATEGORY = BUKKIT.relativeClassName("inventory.recipe", "CraftingBookCategory");

    public static final ClassNamed MAP_PALETTE = BUKKIT.relativeClassName("map", "MapPalette");

    public static final ClassNamed DISPLAY_SLOT = BUKKIT.relativeClassName("scoreboard", "DisplaySlot");

    public static final ClassNamed DAMAGE_TYPE_TAGS = BUKKIT.relativeClassName("tag", "DamageTypeTags");

    public static final ClassNamed BLOCK_DATA = BUKKIT.relativeClassName("block.data", "BlockData");

    public static final ClassNamed BLOCK_DATA_REDSTONE_WIRE = BUKKIT.relativeClassName("block.data.type", "RedstoneWire");

    public static final ClassNamed BLOCK_DATA_MULTIPLE_FACING = BUKKIT.relativeClassName("block.data", "MultipleFacing");

    public static final ClassNamed NAMED_TEXT_COLOR = ClassNamed.of("net.kyori.adventure.text.format", "NamedTextColor");

    public static final ClassNamed REGISTRY_KEY = typed(io.papermc.generator.types.Types.REGISTRY_KEY);

    public static final ClassNamed REGISTRY_EVENTS = PAPER.relativeClassName("registry.event", "RegistryEvents");

    public static final ClassNamed REGISTRY_EVENT_PROVIDER = PAPER.relativeClassName("registry.event", "RegistryEventProvider");

    public static final ClassNamed ITEM_USE_ANIMATION = PAPER.relativeClassName("datacomponent.item.consumable", "ItemUseAnimation");

    public static final ClassNamed GENERATED_FROM = typed(io.papermc.generator.types.Types.GENERATED_FROM);


    public static final ClassNamed CRAFT_BLOCK_DATA = typed(io.papermc.generator.types.Types.CRAFT_BLOCK_DATA);

    public static final ClassNamed CRAFT_BLOCK_STATES = BUKKIT.relativeImpClassName("block", "CraftBlockStates");

    public static final ClassNamed CRAFT_ITEM_METAS =  BUKKIT.relativeImpClassName("inventory", "CraftItemMetas");

    public static final ClassNamed CRAFT_STATISTIC = BUKKIT.relativeImpClassName(null, "CraftStatistic");

    public static final ClassNamed CRAFT_POTION_UTIL = BUKKIT.relativeImpClassName("potion", "CraftPotionUtil");

    public static final ClassNamed FIELD_RENAME = BUKKIT.relativeImpClassName("legacy", "FieldRename");

    public static final ClassNamed REGISTRIES_ARGUMENT_PROVIDER = BUKKIT.relativeClassName("support.provider" , "RegistriesArgumentProvider");

    public static final ClassNamed REGISTRY_CONVERSION_TEST = BUKKIT.relativeClassName("registry", "RegistryConversionTest");

    public static final ClassNamed PAPER_REGISTRIES = PAPER.relativeClassName("registry", "PaperRegistries");

    public static final ClassNamed PAPER_FEATURE_FLAG_PROVIDER_IMPL = PAPER.relativeClassName("world.flag", "PaperFeatureFlagProviderImpl");

    public static final ClassNamed PAPER_SIMPLE_REGISTRY = PAPER.relativeClassName("registry", "PaperSimpleRegistry");

    public static final ClassNamed MOB_GOAL_HELPER = PAPER_LEGACY.relativeClassName("entity.ai", "MobGoalHelper");

    public static ClassNamed typed(ClassName name) {
        List<String> names = new ArrayList<>(name.simpleNames());
        String topName = names.removeFirst();
        return ClassNamed.of(name.packageName(), topName, names.toArray(new String[0]));
    }
}
