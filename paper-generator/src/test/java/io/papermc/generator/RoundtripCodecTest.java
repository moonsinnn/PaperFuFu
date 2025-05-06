package io.papermc.generator;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import com.mojang.serialization.JsonOps;
import io.papermc.generator.registry.RegistryEntry;
import io.papermc.generator.resources.DataFile;
import io.papermc.generator.resources.DataFileLoader;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import io.papermc.generator.resources.DataFiles;
import io.papermc.generator.resources.EntityTypeData;
import io.papermc.generator.resources.ItemMetaData;
import io.papermc.generator.resources.RegistryData;
import io.papermc.generator.utils.predicate.BlockPredicate;
import io.papermc.generator.utils.predicate.BlockPropertyPredicate;
import io.papermc.generator.utils.predicate.ItemPredicate;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.vault.VaultState;
import net.minecraft.world.level.block.state.properties.CreakingHeartState;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static io.papermc.generator.utils.BasePackage.BUKKIT;
import static io.papermc.generator.utils.BasePackage.CRAFT_BUKKIT;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RoundtripCodecTest extends BootstrapTest {

    private static Multimap<ResourceKey<? extends DataFile<?, ?, ?>>, ?> values() {
        Random random = new Random();
        RandomSource randomSource = RandomSource.create();
        RandomStringUtils randomStr = RandomStringUtils.insecure();
        Registry<Item> itemRegistry = Main.REGISTRY_ACCESS.lookupOrThrow(Registries.ITEM);
        Registry<EntityType<?>> entityTypeRegistry = Main.REGISTRY_ACCESS.lookupOrThrow(Registries.ENTITY_TYPE);
        return Util.make(MultimapBuilder.hashKeys().arrayListValues().build(), map -> {
            map.put(DataFiles.BLOCK_STATE_AMBIGUOUS_NAMES, Map.of("Test", List.of("CraftA")));
            map.put(DataFiles.BLOCK_STATE_AMBIGUOUS_NAMES, Map.of("Test", List.of("CraftA", "CraftA")));
            map.put(DataFiles.BLOCK_STATE_AMBIGUOUS_NAMES, Map.of("Test", List.of("CraftA", "CraftB")));
            map.put(DataFiles.BLOCK_STATE_AMBIGUOUS_NAMES, Map.of(randomStr.nextAlphabetic(5), List.of(randomStr.nextAlphabetic(10))));

            map.put(DataFiles.BLOCK_STATE_ENUM_PROPERTY_TYPES, Map.of(VaultState.class, BUKKIT.rootClass("Vault", "State")));
            map.put(DataFiles.BLOCK_STATE_ENUM_PROPERTY_TYPES, Map.of(CreakingHeartState.class, BUKKIT.rootClass("CreakingHeartState")));
            map.put(DataFiles.BLOCK_STATE_ENUM_PROPERTY_TYPES, Map.of(CreakingHeartState.class, BUKKIT.rootClass(randomStr.nextAlphabetic(5))));

            map.put(DataFiles.BLOCK_STATE_PREDICATES, Map.of(BUKKIT.rootClassNamed("BlockData"), List.of(
                new BlockPredicate.IsClassPredicate(VaultBlock.class),
                new BlockPredicate.InstanceOfPredicate(FlowerPotBlock.class, List.of()),
                new BlockPredicate.InstanceOfPredicate(BaseRailBlock.class, List.of(
                    new BlockPropertyPredicate.IsFieldPredicate("POWERED"),
                    new BlockPropertyPredicate.IsNamePredicate("hatch")
                )),
                new BlockPredicate.ContainsPropertyPredicate(List.of(
                    new BlockPropertyPredicate.IsFieldPredicate("POWERED"),
                    new BlockPropertyPredicate.IsNamePredicate("hatch")
                ), 1, BlockPredicate.ContainsPropertyPredicate.Strategy.AT_LEAST),
                new BlockPredicate.ContainsPropertyPredicate(List.of(
                    new BlockPropertyPredicate.IsFieldPredicate("DUSTED"),
                    new BlockPropertyPredicate.IsNamePredicate("age")
                ), 2, BlockPredicate.ContainsPropertyPredicate.Strategy.AT_LEAST),
                new BlockPredicate.ContainsPropertyPredicate(List.of(
                    new BlockPropertyPredicate.IsFieldPredicate("POWER"),
                    new BlockPropertyPredicate.IsNamePredicate("level")
                ), 2, BlockPredicate.ContainsPropertyPredicate.Strategy.EXACT),
                new BlockPredicate.ContainsPropertyPredicate(List.of(
                    new BlockPropertyPredicate.IsFieldPredicate("FACING"),
                    new BlockPropertyPredicate.IsNamePredicate(randomStr.nextAlphabetic(10).toLowerCase(Locale.ROOT)),
                    new BlockPropertyPredicate.IsNamePredicate(randomStr.nextAlphabetic(5).toLowerCase(Locale.ROOT))
                ), random.nextInt(3) + 1, BlockPredicate.ContainsPropertyPredicate.Strategy.values()[random.nextInt(BlockPredicate.ContainsPropertyPredicate.Strategy.values().length)])
            )));

            map.put(DataFiles.ITEM_META_BRIDGE, Map.of(
                CRAFT_BUKKIT.rootClassNamed("CraftSomethingMeta"), new ItemMetaData(
                    BUKKIT.rootClassNamed("SomethingMeta"), "SOMETHING_DATA"
                ),
                CRAFT_BUKKIT.rootClassNamed(randomStr.nextAlphabetic(10)), new ItemMetaData(
                    BUKKIT.rootClassNamed(randomStr.nextAlphabetic(5)), randomStr.nextAlphabetic(10).toUpperCase(Locale.ROOT)
                )
            ));

            map.put(DataFiles.ITEM_META_PREDICATES, Map.of(
                CRAFT_BUKKIT.rootClassNamed("CraftSomethingMeta"), List.of(
                    new ItemPredicate.InstanceOfPredicate(StandingAndWallBlockItem.class, false),
                    new ItemPredicate.InstanceOfPredicate(EntityBlock.class, true),
                    new ItemPredicate.IsClassPredicate(BedItem.class, false),
                    new ItemPredicate.IsClassPredicate(BellBlock.class, true),
                    new ItemPredicate.IsElementPredicate(Either.left(ItemTags.ANVIL)),
                    new ItemPredicate.IsElementPredicate(Either.right(itemRegistry.wrapAsHolder(Items.APPLE))),
                    new ItemPredicate.IsElementPredicate(Either.right(itemRegistry.getRandom(randomSource).orElseThrow()))
                )
            ));

            map.put(DataFiles.registry(RegistryEntry.Type.BUILT_IN), Map.of(
                Registries.GAME_EVENT,
                new RegistryData(
                    new RegistryData.Api(BUKKIT.rootClassNamed("GameEvent")),
                    new RegistryData.Impl(CRAFT_BUKKIT.rootClassNamed("CraftGameEvent")),
                    Optional.empty(),
                    Optional.empty(),
                    false
                ),
                Registries.BLOCK_ENTITY_TYPE,
                new RegistryData(
                    new RegistryData.Api(
                        BUKKIT.rootClassNamed("BlockEntityType"),
                        Optional.of(BUKKIT.rootClassNamed("BlockEntityTypes")),
                        RegistryData.Api.Type.INTERFACE,
                        true,
                        Optional.of("BLOCK_ENTITY_TYPE")
                    ),
                    new RegistryData.Impl(CRAFT_BUKKIT.rootClassNamed("CraftBlockEntityType"), "of", true),
                    Optional.of(
                        new RegistryData.Builder(
                            BUKKIT.rootClassNamed("BlockEntityTypeRegistryBuilder"),
                            BUKKIT.rootClassNamed("PaperBlockEntityTypeRegistryBuilder"),
                            RegistryData.Builder.RegisterCapability.WRITABLE
                        )
                    ),
                    Optional.of("BLOCK_ENTITY_TYPE_RENAME"),
                    true
                ),
                Registries.ATTRIBUTE,
                new RegistryData(
                    new RegistryData.Api(
                        BUKKIT.rootClassNamed("Attribute"),
                        Optional.of(BUKKIT.rootClassNamed("Attributes")),
                        RegistryData.Api.Type.CLASS,
                        false,
                        Optional.of("ATTRIBUTE")
                    ),
                    new RegistryData.Impl(CRAFT_BUKKIT.rootClassNamed("CraftAttribute"), "of", false),
                    Optional.of(
                        new RegistryData.Builder(
                            BUKKIT.rootClassNamed("BlockEntityTypeRegistryBuilder"),
                            BUKKIT.rootClassNamed("PaperBlockEntityTypeRegistryBuilder"),
                            RegistryData.Builder.RegisterCapability.NONE
                        )
                    ),
                    Optional.of("BLOCK_ENTITY_TYPE_RENAME"),
                    false
                )
            ));

            map.put(DataFiles.registry(RegistryEntry.Type.DATA_DRIVEN), Map.of(
                Registries.DATA_COMPONENT_PREDICATE_TYPE,
                new RegistryData(
                    new RegistryData.Api(BUKKIT.rootClassNamed("DataComponentPredicate", "Type")),
                    new RegistryData.Impl(CRAFT_BUKKIT.rootClassNamed("CraftDataComponentPredicate", "CraftType")),
                    Optional.empty(),
                    Optional.empty(),
                    false
                )
            ));

            map.put(DataFiles.ENTITY_TYPES, Map.of(
                entityTypeRegistry.getResourceKey(EntityType.HUSK).orElseThrow(),
                new EntityTypeData(BUKKIT.rootClassNamed("Husk"))
            ));
            map.put(DataFiles.ENTITY_TYPES, Map.of(
                entityTypeRegistry.getResourceKey(EntityType.ZOGLIN).orElseThrow(),
                new EntityTypeData(BUKKIT.rootClassNamed("Zoglin"), -1)
            ));
            map.put(DataFiles.ENTITY_TYPES, Map.of(
                entityTypeRegistry.getResourceKey(EntityType.END_CRYSTAL).orElseThrow(),
                new EntityTypeData(BUKKIT.rootClassNamed("EndCrystal"), 5)
            ));
            map.put(DataFiles.ENTITY_TYPES, Map.of(
                entityTypeRegistry.getRandom(randomSource).orElseThrow().key(),
                new EntityTypeData(BUKKIT.rootClassNamed(randomStr.nextAlphabetic(5)), randomSource.nextIntBetweenInclusive(-1, 100))
            ));

            map.put(DataFiles.ENTITY_CLASS_NAMES, Map.of(
                Zombie.class,
                BUKKIT.rootClass("Zombie")
            ));
        });
    }

    public static Stream<Arguments> data() {
        Set<DynamicOps<?>> ops = Stream.of(
            JavaOps.INSTANCE,
            JsonOps.INSTANCE
        ).map(op -> RegistryOps.create(op, Main.REGISTRY_ACCESS)).collect(Collectors.toSet());
        Multimap<ResourceKey<? extends DataFile<?, ?, ?>>, ?> values = values();

        return DataFileLoader.DATA_FILES_VIEW.entrySet().stream()
            .flatMap(entry -> ops.stream().flatMap(op ->
                values.get(entry.getKey()).stream().map(v -> Arguments.of(op, entry.getValue().codec(), v))));
    }

    @ParameterizedTest
    @MethodSource("data")
    public <T, V> void testCodec(DynamicOps<T> ops, Codec<V> codec, V value) {
        DataResult<T> encoded = codec.encodeStart(ops, value);
        DataResult<V> decoded = encoded.flatMap(r -> codec.parse(ops, r));
        assertEquals(DataResult.success(value), decoded, "read(write(x)) == x");

        DataResult<T> reEncoded = decoded.flatMap(r -> codec.encodeStart(ops, r));
        assertEquals(encoded, reEncoded, "write(read(x)) == x");
    }
}
