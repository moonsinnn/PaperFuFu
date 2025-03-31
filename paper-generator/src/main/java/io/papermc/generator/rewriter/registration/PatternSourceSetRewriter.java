package io.papermc.generator.rewriter.registration;

import io.papermc.generator.registry.RegistryEntries;
import io.papermc.generator.rewriter.types.registry.RegistryIdentifiable;
import io.papermc.typewriter.ClassNamed;
import io.papermc.typewriter.registration.SourceSetRewriter;
import io.papermc.typewriter.replace.CompositeRewriter;
import io.papermc.typewriter.replace.SearchReplaceRewriter;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface PatternSourceSetRewriter extends SourceSetRewriter<PatternSourceSetRewriter> {

    default PatternSourceSetRewriter register(String pattern, Class<?> targetClass, SearchReplaceRewriter rewriter) {
        return register(pattern, new ClassNamed(targetClass), rewriter);
    }

    default <E, T extends SearchReplaceRewriter & RegistryIdentifiable<E>> PatternSourceSetRewriter register(String pattern, T rewriter) {
        return this.register(pattern, RegistryEntries.byRegistryKey(rewriter.getRegistryKey()).data().api().klass(), rewriter);
    }

    PatternSourceSetRewriter register(String pattern, ClassNamed targetClass, SearchReplaceRewriter rewriter);

    default PatternSourceSetRewriter register(Class<?> mainClass, CompositeRewriter rewriter) {
        return this.register(new ClassNamed(mainClass), rewriter);
    }

    PatternSourceSetRewriter register(ClassNamed mainClass, CompositeRewriter rewriter);
}
