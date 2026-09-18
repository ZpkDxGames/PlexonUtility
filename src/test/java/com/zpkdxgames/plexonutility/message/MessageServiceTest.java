package com.zpkdxgames.plexonutility.message;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageServiceTest {
    @Test
    void bundledLeafStringsDefineTheSchemaIncludingNestedKeys() {
        YamlConfiguration defaults = defaultsCatalog("default");

        Set<String> keys = MessageService.requiredKeys(defaults);

        assertEquals(Set.of("prefix", "nested.one", "nested.two"), keys);
    }

    @Test
    void existingCatalogCopiesEveryMissingBundledLeafAndPreservesCustomValues() {
        YamlConfiguration existing = new YamlConfiguration();
        existing.set("prefix", "custom");
        YamlConfiguration defaults = defaultsCatalog("default");

        int migrated = MessageService.applyDefaultsAndValidate(existing, defaults);

        assertEquals(2, migrated);
        assertEquals("custom", existing.getString("prefix"));
        assertEquals("default", existing.getString("nested.one"));
        assertEquals("default", existing.getString("nested.two"));
    }

    @Test
    void completeCatalogDoesNotReportMigration() {
        YamlConfiguration existing = defaultsCatalog("existing");
        YamlConfiguration defaults = defaultsCatalog("default");

        assertEquals(0, MessageService.applyDefaultsAndValidate(existing, defaults));
        assertEquals("existing", existing.getString("nested.one"));
    }

    @Test
    void rejectsWrongMessageTypeEvenWhenBundledDefaultExists() {
        YamlConfiguration existing = defaultsCatalog("custom");
        existing.set("nested.one", 42);

        assertThrows(IllegalArgumentException.class,
                () -> MessageService.applyDefaultsAndValidate(existing, defaultsCatalog("default")));
    }

    @Test
    void acceptsCompleteStringCatalog() {
        YamlConfiguration yaml = defaultsCatalog("custom");
        assertDoesNotThrow(() -> MessageService.validateCatalog(yaml));
        assertTrue(MessageService.requiredKeys(yaml).contains("nested.two"));
    }

    private static YamlConfiguration defaultsCatalog(String value) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("prefix", value);
        yaml.set("nested.one", value);
        yaml.set("nested.two", value);
        return yaml;
    }
}
