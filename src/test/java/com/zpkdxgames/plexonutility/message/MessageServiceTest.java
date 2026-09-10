package com.zpkdxgames.plexonutility.message;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessageServiceTest {
    @Test
    void acceptsCompleteStringCatalog() {
        YamlConfiguration yaml = completeCatalog();
        assertDoesNotThrow(() -> MessageService.validateCatalog(yaml));
    }

    @Test
    void rejectsMissingRequiredMessage() {
        YamlConfiguration yaml = completeCatalog();
        yaml.set("heal-unavailable", null);
        assertThrows(IllegalArgumentException.class, () -> MessageService.validateCatalog(yaml));
    }

    @Test
    void rejectsWrongMessageType() {
        YamlConfiguration yaml = completeCatalog();
        yaml.set("cooldown", 42);
        assertThrows(IllegalArgumentException.class, () -> MessageService.validateCatalog(yaml));
    }

    private static YamlConfiguration completeCatalog() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (String key : MessageService.REQUIRED_KEYS) yaml.set(key, "ok");
        return yaml;
    }
}
