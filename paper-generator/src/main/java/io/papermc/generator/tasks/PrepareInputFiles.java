package io.papermc.generator.tasks;

import com.mojang.logging.LogUtils;
import io.papermc.generator.Main;
import io.papermc.generator.resources.DataFile;
import io.papermc.generator.resources.DataFileLoader;
import io.papermc.generator.resources.MutationResult;
import org.slf4j.Logger;
import java.io.IOException;
import java.nio.file.Path;

public class PrepareInputFiles {

    static {
        Main.bootStrap(true);
    }

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void main(String[] args) throws IOException {
        Path resourceDir = Path.of(args[0]);
        for (DataFile<?, ?, ?> file : DataFileLoader.DATA_FILES) {
            if (!file.isMutable()) {
                continue;
            }

            Path filePath = Path.of(file.path());
            MutationResult<?, ?, ?> result = file.upgrade(resourceDir.resolve(filePath));

            if (!result.added().isEmpty()) {
                LOGGER.info("Added the following elements in {}:", filePath);
                result.added().stream().map(o -> "- " + o).forEach(LOGGER::info);
            }
            if (!result.removed().isEmpty()) {
                LOGGER.warn("Removed the following keys in {}:", filePath);
                result.removed().stream().map(o -> "- " + o).forEach(LOGGER::warn);
            }
        }
    }
}
