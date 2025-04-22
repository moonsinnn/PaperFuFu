package io.papermc.generator.types.craftblockdata.property.appender;

import com.squareup.javapoet.ClassName;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface PropertyAppender<T extends Comparable<T>> extends AppenderBase {

    Property<T> getProperty();

    ClassName getApiType();
}
