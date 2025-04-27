package io.papermc.generator.resources;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface Transmuter<V, A, R> {

    MutationResult<V, A, R> transmute(V value);

    interface Mutable<V, A, R> extends Transmuter<V, A, R> {

        V applyChanges(MutationResult<V, A, R> result);
    }
}
