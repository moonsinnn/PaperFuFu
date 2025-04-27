package io.papermc.generator.types.craftblockdata.property.holder.appender;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.papermc.generator.resources.DataFileLoader;
import io.papermc.generator.types.Types;
import io.papermc.generator.types.craftblockdata.CraftBlockDataGenerator;
import io.papermc.generator.types.craftblockdata.property.converter.ConverterBase;
import io.papermc.generator.types.craftblockdata.property.holder.DataHolderType;
import io.papermc.generator.utils.CommonVariable;
import io.papermc.generator.utils.NamingManager;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class MapAppender implements DataAppender {

    private static final Map<String, String> INDEX_NAMES = ImmutableMap.<String, String>builder()
        .put(Types.BLOCK_FACE.simpleName(), "Face")
        .buildOrThrow();
    // todo find another way for extra method and clean up those hardcoded things ^

    @Override
    public DataHolderType getType() {
        return DataHolderType.MAP;
    }

    @Override
    public void addExtras(TypeSpec.Builder builder, FieldSpec field, ParameterSpec indexParameter, ConverterBase childConverter, CraftBlockDataGenerator generator, NamingManager baseNaming) {
        if (childConverter.getApiType().equals(TypeName.BOOLEAN)) {
            String collectVarName = baseNaming.getVariableNameWrapper().post("s").concat();
            MethodSpec.Builder methodBuilder = generator.createMethod(baseNaming.getMethodNameWrapper().post("s").concat());
            methodBuilder.addStatement("$T $L = $T.builder()", ParameterizedTypeName.get(ClassName.get(ImmutableSet.Builder.class), indexParameter.type), collectVarName, ImmutableSet.class);
            methodBuilder.beginControlFlow("for ($T $N : $N.entrySet())", ParameterizedTypeName.get(ClassName.get(Map.Entry.class), indexParameter.type, TypeName.get(BooleanProperty.class)), CommonVariable.MAP_ENTRY, field);
            {
                methodBuilder.beginControlFlow("if (" + childConverter.rawGetExprent().formatted("$L.getValue()") + ")", CommonVariable.MAP_ENTRY);
                {
                    methodBuilder.addStatement("$L.add($N.getKey())", collectVarName, CommonVariable.MAP_ENTRY);
                }
                methodBuilder.endControlFlow();
            }
            methodBuilder.endControlFlow();
            methodBuilder.addStatement("return $L.build()", collectVarName);
            methodBuilder.returns(ParameterizedTypeName.get(ClassName.get(Set.class), indexParameter.type));

            builder.addMethod(methodBuilder.build());
        }

        if (DataFileLoader.BlockState.EXTRA_ALLOWED_METHOD.get().contains(generator.getBaseClass()) &&
            indexParameter.type instanceof ClassName className && !className.isBoxedPrimitive()) {
            NamingManager.NameWrapper indexNaming = NamingManager.NameWrapper.wrap("get", INDEX_NAMES.getOrDefault(className.simpleName(), className.simpleName()));

            MethodSpec.Builder methodBuilder = generator.createMethod(indexNaming.pre("Allowed").post("s").concat());
            methodBuilder.addStatement("return $T.unmodifiableSet($N.keySet())", Collections.class, field);
            methodBuilder.returns(ParameterizedTypeName.get(ClassName.get(Set.class), className));

            builder.addMethod(methodBuilder.build());
        }
    }
}
