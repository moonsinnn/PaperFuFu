package io.papermc.generator;

import io.papermc.generator.resources.DataFile;
import io.papermc.generator.resources.DataFileLoader;
import io.papermc.generator.resources.MutationResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataFileTest extends BootstrapTest {

    public static List<DataFile<?, ?, ?>> files() {
        return DataFileLoader.DATA_FILES;
    }

    @ParameterizedTest
    @MethodSource("files")
    public <V, A, R> void testFile(DataFile<V, A, R> file) {
        MutationResult<V, A, R> result = file.transmuter().transmute(file.readUnchecked());
        assertTrue(result.added().isEmpty(), () -> "Missing some data in " + file.path() + ":\n" + String.join("\n", result.added().stream().map(Object::toString).collect(Collectors.toSet())));
        assertTrue(result.removed().isEmpty(), () -> "Extra data found in " + file.path() + ":\n" + String.join("\n", result.removed().stream().map(Object::toString).collect(Collectors.toSet())));
    }
}
