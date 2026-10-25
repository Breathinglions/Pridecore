package com.prideregion.core;

import java.io.IOException;
import java.nio.file.*;
import java.util.Properties;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;

public record PrideCoreConfig(boolean allowAdminMutations) {
    public static PrideCoreConfig load(Path configDir) throws IOException {
        Files.createDirectories(configDir);
        Path file = configDir.resolve("pride_core.properties");
        if (!Files.exists(file)) {
            try {
                Files.writeString(file, "# Pride Core foundation: default read-only. Enable ONLY on a backed-up staging world.\nallow_admin_mutations=false\n", CREATE_NEW, WRITE);
            } catch (FileAlreadyExistsException ignored) {
                // Another process created it; read the existing value instead.
            }
        }
        Properties properties = new Properties();
        try (var in = Files.newInputStream(file)) { properties.load(in); }
        return new PrideCoreConfig("true".equalsIgnoreCase(
                properties.getProperty("allow_admin_mutations", "false").trim()));
    }
}
