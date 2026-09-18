package com.zpkdxgames.plexonutility.admin.entity;

import org.bukkit.Server;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WildStackerAdapterTest {
    @Test
    void absentWildStackerUsesVanillaRemoval() {
        Fixture fixture = fixture(null);
        LivingEntity entity = mock(LivingEntity.class);

        WildStackerAdapter.Inspection inspection = fixture.adapter().inspect(entity);
        WildStackerAdapter.Removal removal = fixture.adapter().remove(entity, inspection);

        assertEquals(WildStackerAdapter.Mode.VANILLA, inspection.mode());
        assertTrue(removal.removed());
        assertEquals(1, removal.logicalAmount());
        verify(entity).remove();
    }

    @Test
    void enabledButUnresolvableWildStackerFailsClosedForLivingEntities() {
        Plugin wildStacker = mock(Plugin.class);
        when(wildStacker.isEnabled()).thenReturn(true);
        Fixture fixture = fixture(wildStacker);
        LivingEntity entity = mock(LivingEntity.class);

        WildStackerAdapter.Inspection inspection = fixture.adapter().inspect(entity);
        WildStackerAdapter.Removal removal = fixture.adapter().remove(entity, inspection);

        assertEquals(WildStackerAdapter.Mode.PROTECT, inspection.mode());
        assertFalse(removal.removed());
        assertEquals(0, removal.logicalAmount());
        verify(entity, never()).remove();
    }

    private static Fixture fixture(Plugin wildStacker) {
        JavaPlugin plugin = mock(JavaPlugin.class);
        Server server = mock(Server.class);
        PluginManager manager = mock(PluginManager.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(manager);
        when(manager.getPlugin("WildStacker")).thenReturn(wildStacker);
        return new Fixture(new WildStackerAdapter(plugin));
    }

    private record Fixture(WildStackerAdapter adapter) { }
}
