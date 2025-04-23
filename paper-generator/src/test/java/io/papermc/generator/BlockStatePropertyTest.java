package io.papermc.generator;

import io.papermc.generator.utils.BlockStateData;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import io.papermc.generator.utils.ClassHelper;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.MossyCarpetBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class BlockStatePropertyTest extends BootstrapTest {

    private static Set<Class<? extends Enum<? extends StringRepresentable>>> ENUM_PROPERTY_TYPES;

    @BeforeAll
    public static void getAllProperties() {
        // get all properties
        Set<Class<? extends Enum<? extends StringRepresentable>>> enumPropertyTypes = Collections.newSetFromMap(new IdentityHashMap<>());
        try {
            for (Field field : BlockStateProperties.class.getDeclaredFields()) {
                if (ClassHelper.isStaticConstant(field, Modifier.PUBLIC)) {
                    if (!EnumProperty.class.isAssignableFrom(field.getType())) {
                        continue;
                    }

                    enumPropertyTypes.add(((EnumProperty<?>) field.get(null)).getValueClass());
                }
            }
            ENUM_PROPERTY_TYPES = Collections.unmodifiableSet(enumPropertyTypes);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testReferences() throws NoSuchFieldException, IllegalAccessException {
        // if renamed should change DataPropertyWriter#FIELD_TO_BASE_NAME/FIELD_TO_BASE_NAME_SPECIFICS
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        lookup.findStaticVarHandle(ChiseledBookShelfBlock.class, "SLOT_OCCUPIED_PROPERTIES", List.class);
        lookup.findStaticVarHandle(PipeBlock.class, "PROPERTY_BY_DIRECTION", Map.class);
        lookup.findStaticVarHandle(WallBlock.class, "PROPERTY_BY_DIRECTION", Map.class);
        lookup.findStaticVarHandle(MossyCarpetBlock.class, "PROPERTY_BY_DIRECTION", Map.class);
    }

    @Test
    public void testBridge() {
        Set<Class<? extends Enum<? extends StringRepresentable>>> missingApiEquivalents = new HashSet<>();
        for (Class<? extends Enum<? extends StringRepresentable>> type : ENUM_PROPERTY_TYPES) {
            if (!BlockStateData.ENUM_PROPERTY_TYPES.containsKey(type)) {
                missingApiEquivalents.add(type);
            }
        }

        Assertions.assertTrue(missingApiEquivalents.isEmpty(), () -> "Missing some api equivalent in the block state data enum types (in block_state/enum_property_types.json) : " + missingApiEquivalents.stream().map(Class::getCanonicalName).collect(Collectors.joining(", ")));
    }
}
