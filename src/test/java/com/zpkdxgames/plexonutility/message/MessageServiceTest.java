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
    void existingCatalogCopiesNewKeysFromBundledDefaults() {
        YamlConfiguration existing = completeCatalog("existing");
        existing.set("heal-unavailable", null);
        YamlConfiguration defaults = completeCatalog("default");

        int migrated = MessageService.applyDefaultsAndValidate(existing, defaults);

        assertEquals(1, migrated);
        assertEquals("default", existing.getString("heal-unavailable"));
        assertEquals("existing", existing.getString("heal-self"));
        assertEquals("default", existing.getValues(false).get("heal-unavailable"));
    }

    @Test
    void completeCatalogDoesNotReportMigration() {
        YamlConfiguration existing = completeCatalog("existing");
        YamlConfiguration defaults = completeCatalog("default");

        assertEquals(0, MessageService.applyDefaultsAndValidate(existing, defaults));
        assertEquals("existing", existing.getString("afk-self-on"));
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
