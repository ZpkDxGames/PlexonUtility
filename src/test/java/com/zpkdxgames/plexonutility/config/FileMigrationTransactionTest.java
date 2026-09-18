package com.zpkdxgames.plexonutility.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileMigrationTransactionTest {
    @TempDir Path tempDir;

    @Test
    void laterCommitFailureRestoresAlreadyReplacedOperatorFile() throws Exception {
        Path config = tempDir.resolve("config.yml");
        Files.writeString(config, "before-config\n");

        Path blockedMessages = tempDir.resolve("messages.yml");
        Files.createDirectories(blockedMessages);
        Files.writeString(blockedMessages.resolve("keep.txt"), "prevents directory replacement");

        List<FileMigrationTransaction.Update> updates = List.of(
                new FileMigrationTransaction.Update(config, "after-config\n", 1, true),
                new FileMigrationTransaction.Update(blockedMessages, "after-messages\n", 1, false));

        assertThrows(IllegalArgumentException.class, () -> FileMigrationTransaction.commit(updates));

        assertEquals("before-config\n", Files.readString(config));
        assertTrue(Files.isDirectory(blockedMessages));
        assertTrue(Files.exists(blockedMessages.resolve("keep.txt")));
        assertFalse(Files.exists(tempDir.resolve("config.yml.tmp")));
        assertFalse(Files.exists(tempDir.resolve("messages.yml.tmp")));
        assertEquals("before-config\n", Files.readString(tempDir.resolve("config.yml.bak")));
    }

    @Test
    void zeroMigrationUpdatesDoNotTouchDisk() {
        Path target = tempDir.resolve("config.yml");

        FileMigrationTransaction.commit(List.of(
                new FileMigrationTransaction.Update(target, "content", 0, false)));

        assertFalse(Files.exists(target));
        assertFalse(Files.exists(tempDir.resolve("config.yml.tmp")));
    }
}
