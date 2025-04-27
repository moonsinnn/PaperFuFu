package io.papermc.generator.resources;

import org.jspecify.annotations.NullMarked;
import java.util.Collections;
import java.util.Set;

@NullMarked
public record MutationResult<V, A, R>(V value, Set<A> added, Set<R> removed) {

    public static <V, A, R> MutationResult<V, A, R> constant(V value) {
        return new MutationResult<>(value, Collections.emptySet(), Collections.emptySet());
    }
}
