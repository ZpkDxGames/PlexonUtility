package com.zpkdxgames.plexonutility.message;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessageServiceTest {
    @Test
    void acceptsCompleteStringCatalog() {
        YamlConfiguration yaml = completeCatalog("custom");
        assertDoesNotThrow(() -> MessageService.validateCatalog(yaml));
    }

    @Test
    void existingCatalogCanInheritNewKeysFromBundledDefaults() {
        YamlConfiguration existing = completeCatalog("existing");
        existing.set("heal-unavailable", null);
        YamlConfiguration defaults = completeCatalog("default");

        assertDoesNotThrow(() -> MessageService.applyDefaultsAndValidate(existing, defaults));
        assertEquals("default", existing.getString("heal-unavailable"));
        assertEquals("existing", existing.getString("heal-self"));
    }

    @Test
    void rejectsMissingRequiredMessageWhenNoDefaultExists() {
        YamlConfiguration yaml = completeCatalog("ok");
        yaml.set("heal-unavailable", null);
        assertThrows(IllegalArgumentException.class, () -> MessageService.validateCatalog(yaml));
    }

    @Test
    void rejectsWrongMessageTypeEvenWhenDefaultsExist() {
        YamlConfiguration yaml = completeCatalog("custom");
        yaml.set("cooldown", 42);
        YamlConfiguration defaults = completeCatalog("default");
        assertThrows(IllegalArgumentException.class, () -> MessageService.applyDefaultsAndValidate(yaml, defaults));
    }

    private static YamlConfiguration completeCatalog(String value) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (String key : MessageService.REQUIRED_KEYS) yaml.set(key, value);
        return yaml;
    }
}
