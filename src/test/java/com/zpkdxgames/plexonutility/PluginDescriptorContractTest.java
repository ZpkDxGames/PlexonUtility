package com.zpkdxgames.plexonutility;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginDescriptorContractTest {
    @Test
    void progressionSelfPermissionsAndAfkBypassDefaultFalse() throws Exception {
        String descriptor = descriptor();

        for (String node : new String[] {
                "plexonutility.feed",
                "plexonutility.heal",
                "plexonutility.enderchest",
                "plexonutility.fly",
                "plexonutility.afk.auto.bypass",
                "plexonutility.admin.killall.bypass-confirm"
        }) {
            assertTrue(permissionDefault(descriptor, node, "false"),
                    () -> node + " must default false");
        }
    }

    @Test
    void adminUmbrellaDoesNotInheritDangerousKillallBypass() throws Exception {
        String descriptor = descriptor();
        String adminChildren = section(descriptor,
                "  plexonutility.admin:\n",
                "  plexonutility.admin.menu:\n");

        assertFalse(adminChildren.contains("plexonutility.admin.killall.bypass-confirm: true"));
        assertTrue(adminChildren.contains("plexonutility.admin.killall: true"));
        assertTrue(adminChildren.contains("plexonutility.fly: true"));
    }

    @Test
    void travelSpawnIsAbsentAndWildStackerIsOptional() throws Exception {
        String descriptor = descriptor();

        assertFalse(Pattern.compile("(?m)^  spawn:\\s*$").matcher(descriptor).find());
        assertTrue(Pattern.compile("(?m)^  spawnmob:\\s*$").matcher(descriptor).find());
        assertTrue(descriptor.contains("softdepend:"));
        assertTrue(descriptor.contains("  - WildStacker"));
    }

    private static boolean permissionDefault(String descriptor, String node, String value) {
        Pattern pattern = Pattern.compile("(?m)^  " + Pattern.quote(node)
                + ":\\s*\\R(?:    .*\\R)*?    default: " + Pattern.quote(value) + "\\s*$");
        return pattern.matcher(descriptor).find();
    }

    private static String section(String source, String start, String end) {
        int from = source.indexOf(start);
        int to = source.indexOf(end, from + start.length());
        if (from < 0 || to < 0) throw new AssertionError("Descriptor section not found");
        return source.substring(from, to);
    }

    private static String descriptor() throws IOException {
        try (InputStream stream = PluginDescriptorContractTest.class.getClassLoader().getResourceAsStream("plugin.yml")) {
            if (stream == null) throw new AssertionError("plugin.yml missing from test classpath");
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
