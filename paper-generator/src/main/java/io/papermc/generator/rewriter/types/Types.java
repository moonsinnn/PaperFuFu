package io.papermc.generator.rewriter.types;

import com.squareup.javapoet.ClassName;
import io.papermc.typewriter.ClassNamed;
import org.jspecify.annotations.NullMarked;
import java.util.ArrayList;
import java.util.List;

@NullMarked
public final class Types {

    public static final String API_PACKAGE = "org.bukkit";
    public static final String PAPER_PACKAGE = "io.papermc.paper";

    public static final ClassNamed BUKKIT = ClassNamed.of(API_PACKAGE, "Bukkit");

    public static final ClassNamed FEATURE_FLAG = ClassNamed.of(API_PACKAGE, "FeatureFlag");

    public static final ClassNamed REGISTRY = ClassNamed.of(API_PACKAGE, "Registry");

    public static final ClassNamed TAG = ClassNamed.of(API_PACKAGE, "Tag");

    public static final ClassNamed NAMESPACED_KEY = typed(io.papermc.generator.types.Types.NAMESPACED_KEY);

    public static final ClassNamed MINECRAFT_EXPERIMENTAL = typed(io.papermc.generator.types.Types.MINECRAFT_EXPERIMENTAL);
    public static final ClassNamed MINECRAFT_EXPERIMENTAL_REQUIRES = typed(io.papermc.generator.types.Types.MINECRAFT_EXPERIMENTAL_REQUIRES);

    @Deprecated
    public static final ClassNamed STATISTIC = ClassNamed.of(API_PACKAGE, "Statistic");

    public static final ClassNamed STATISTIC_TYPE = ClassNamed.of(API_PACKAGE, "Statistic", "Type");

    public static final ClassNamed BLOCK_TYPE_TYPED = ClassNamed.of(API_PACKAGE + ".block", "BlockType", "Typed");

    public static final ClassNamed ITEM_TYPE_TYPED = ClassNamed.of(API_PACKAGE + ".inventory", "ItemType", "Typed"); // todo add nested util to ClassNamed and get it from registry entries

    public static final ClassNamed ITEM_META = ClassNamed.of(API_PACKAGE + ".inventory.meta", "ItemMeta");

    public static final ClassNamed LOCATION = ClassNamed.of(API_PACKAGE, "Location");

    public static final ClassNamed MATERIAL = ClassNamed.of(API_PACKAGE, "Material");

    public static final ClassNamed POSE = ClassNamed.of(API_PACKAGE + ".entity", "Pose");

    @Deprecated
    public static final ClassNamed VILLAGER = ClassNamed.of(API_PACKAGE + ".entity", "Villager");

    public static final ClassNamed SNIFFER_STATE = ClassNamed.of(API_PACKAGE + ".entity", "Sniffer", "State");

    public static final ClassNamed TROPICAL_FISH_PATTERN = ClassNamed.of(API_PACKAGE + ".entity", "TropicalFish", "Pattern");

    public static final ClassNamed FOX_TYPE = ClassNamed.of(API_PACKAGE + ".entity", "Fox", "Type");

    public static final ClassNamed SALMON_VARIANT = ClassNamed.of(API_PACKAGE + ".entity", "Salmon", "Variant");

    public static final ClassNamed PANDA_GENE = ClassNamed.of(API_PACKAGE + ".entity", "Panda", "Gene");

    public static final ClassNamed BOAT_STATUS = ClassNamed.of(API_PACKAGE + ".entity", "Boat", "Status");

    public static final ClassNamed ITEM_RARITY = ClassNamed.of(API_PACKAGE + ".inventory", "ItemRarity");

    public static final ClassNamed COOKING_BOOK_CATEGORY = ClassNamed.of(API_PACKAGE + ".inventory.recipe", "CookingBookCategory");

    public static final ClassNamed CRAFTING_BOOK_CATEGORY = ClassNamed.of(API_PACKAGE + ".inventory.recipe", "CraftingBookCategory");

    public static final ClassNamed MAP_PALETTE = ClassNamed.of(API_PACKAGE + ".map", "MapPalette");

    public static final ClassNamed DISPLAY_SLOT = ClassNamed.of(API_PACKAGE + ".scoreboard", "DisplaySlot");

    public static final ClassNamed DAMAGE_TYPE_TAGS = ClassNamed.of(API_PACKAGE + ".tag", "DamageTypeTags");

    public static final ClassNamed BLOCK_DATA = ClassNamed.of(API_PACKAGE + ".block.data", "BlockData");

    public static final ClassNamed BLOCK_DATA_REDSTONE_WIRE = ClassNamed.of(API_PACKAGE + ".block.data.type", "RedstoneWire");

    public static final ClassNamed BLOCK_DATA_MULTIPLE_FACING = ClassNamed.of(API_PACKAGE + ".block.data", "MultipleFacing");

    public static final ClassNamed NAMED_TEXT_COLOR = ClassNamed.of("net.kyori.adventure.text.format", "NamedTextColor");

    public static final ClassNamed REGISTRY_KEY = typed(io.papermc.generator.types.Types.REGISTRY_KEY);

    public static final ClassNamed REGISTRY_EVENTS = ClassNamed.of(PAPER_PACKAGE + ".registry.event", "RegistryEvents");

    public static final ClassNamed REGISTRY_EVENT_PROVIDER = ClassNamed.of(PAPER_PACKAGE + ".registry.event", "RegistryEventProvider");

    public static final ClassNamed ITEM_USE_ANIMATION = ClassNamed.of(PAPER_PACKAGE + ".datacomponent.item.consumable", "ItemUseAnimation");

    public static final ClassNamed GENERATED_FROM = typed(io.papermc.generator.types.Types.GENERATED_FROM);


    public static final String IMPL_PACKAGE = "org.bukkit.craftbukkit";

    public static final ClassNamed CRAFT_BLOCK_DATA = typed(io.papermc.generator.types.Types.CRAFT_BLOCK_DATA);

    public static final ClassNamed CRAFT_BLOCK_STATES = ClassNamed.of(IMPL_PACKAGE + ".block", "CraftBlockStates");

    public static final ClassNamed CRAFT_ITEM_METAS = ClassNamed.of(IMPL_PACKAGE + ".inventory", "CraftItemMetas");

    public static final ClassNamed CRAFT_STATISTIC = ClassNamed.of(IMPL_PACKAGE, "CraftStatistic");

    public static final ClassNamed CRAFT_POTION_UTIL = ClassNamed.of(IMPL_PACKAGE + ".potion", "CraftPotionUtil");

    public static final ClassNamed FIELD_RENAME = ClassNamed.of(IMPL_PACKAGE + ".legacy", "FieldRename");

    public static final ClassNamed REGISTRIES_ARGUMENT_PROVIDER = ClassNamed.of("org.bukkit.support.provider" , "RegistriesArgumentProvider");

    public static final ClassNamed REGISTRY_CONVERSION_TEST = ClassNamed.of("org.bukkit.registry" , "RegistryConversionTest");

    public static final ClassNamed PAPER_REGISTRIES = ClassNamed.of(PAPER_PACKAGE + ".registry", "PaperRegistries");

    public static final ClassNamed PAPER_FEATURE_FLAG_PROVIDER_IMPL = ClassNamed.of(PAPER_PACKAGE + ".world.flag", "PaperFeatureFlagProviderImpl");

    public static final ClassNamed PAPER_SIMPLE_REGISTRY = ClassNamed.of(PAPER_PACKAGE + ".registry", "PaperSimpleRegistry");

    public static final ClassNamed MOB_GOAL_HELPER = ClassNamed.of("com.destroystokyo.paper.entity.ai", "MobGoalHelper");

    public static ClassNamed typed(ClassName name) {
        List<String> names = new ArrayList<>(name.simpleNames());
        String topName = names.removeFirst();
        return ClassNamed.of(name.packageName(), topName, names.toArray(new String[0]));
    }
}
