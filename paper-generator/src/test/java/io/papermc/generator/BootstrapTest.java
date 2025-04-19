package io.papermc.generator;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

public class BootstrapTest {

    static {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Bootstrap.validate();
    }
}
