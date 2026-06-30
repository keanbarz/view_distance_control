package org.rainbowhunter.viewdistancecontrol;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

class ViewDistanceManagerTest {

    private ConfigManager config;
    private JavaPlugin plugin;
    private ViewDistanceManager manager;
    private Player player;
    private UUID playerId;
    private MockedStatic<Bukkit> bukkit;
    private Server server;
    private Logger logger;

    @BeforeEach
    void setUp() {
        config = mock(ConfigManager.class);
        when(config.getDefaultViewDistance()).thenReturn(10);
        when(config.getDefaultAfkViewDistance()).thenReturn(4);
        when(config.isAfkEnabled()).thenReturn(true);
        when(config.isNotifyPlayer()).thenReturn(false);

        plugin = mock(JavaPlugin.class);
        logger = mock(Logger.class);
        when(plugin.getLogger()).thenReturn(logger);
        manager = new ViewDistanceManager(plugin, config);

        playerId = UUID.randomUUID();
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getEffectivePermissions()).thenReturn(Set.of());
        when(player.getSendViewDistance()).thenReturn(0);

        server = mock(Server.class);
        when(server.getViewDistance()).thenReturn(32);

        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getServer).thenReturn(server);
        bukkit.when(() -> Bukkit.getPlayer(playerId)).thenReturn(player);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    void noPermission_usesConfigDefault() {
        manager.applyViewDistance(player);
        verify(player).setSendViewDistance(10);
    }

    @Test
    void singlePermissionNode_usedDirectly() {
        when(player.getEffectivePermissions()).thenReturn(Set.of(
                perm("viewdistancecontrol.default.12")
        ));
        manager.applyViewDistance(player);
        verify(player).setSendViewDistance(12);
    }

    @Test
    void multipleNodes_highestWins() {
        when(player.getEffectivePermissions()).thenReturn(Set.of(
                perm("viewdistancecontrol.default.8"),
                perm("viewdistancecontrol.default.16"),
                perm("viewdistancecontrol.default.12")
        ));
        manager.applyViewDistance(player);
        verify(player).setSendViewDistance(16);
    }

    @Test
    void negatedNode_ignored() {
        when(player.getEffectivePermissions()).thenReturn(Set.of(
                new PermissionAttachmentInfo(player, "viewdistancecontrol.default.12", null, false)
        ));
        manager.applyViewDistance(player);
        verify(player).setSendViewDistance(10);
    }

    @Test
    void afkPlayer_usesAfkConfigDefault() {
        manager.setAfk(playerId, true);
        verify(player).setSendViewDistance(4);
    }

    @Test
    void afkPlayer_usesAfkPermissionNode() {
        when(player.getEffectivePermissions()).thenReturn(Set.of(
                perm("viewdistancecontrol.afk.2")
        ));
        manager.setAfk(playerId, true);
        verify(player).setSendViewDistance(2);
    }

    @Test
    void afkPlayer_highestAfkNodeWins() {
        when(player.getEffectivePermissions()).thenReturn(Set.of(
                perm("viewdistancecontrol.afk.2"),
                perm("viewdistancecontrol.afk.6")
        ));
        manager.setAfk(playerId, true);
        verify(player).setSendViewDistance(6);
    }

    @Test
    void afkDisabled_normalDistanceUsedWhileAfk() {
        when(config.isAfkEnabled()).thenReturn(false);
        when(player.getEffectivePermissions()).thenReturn(Set.of(
                perm("viewdistancecontrol.default.12")
        ));
        manager.setAfk(playerId, true);
        verify(player).setSendViewDistance(12);
    }

    @Test
    void returnFromAfk_restoresNormalDistance() {
        when(player.getEffectivePermissions()).thenReturn(Set.of(
                perm("viewdistancecontrol.default.12"),
                perm("viewdistancecontrol.afk.2")
        ));
        manager.setAfk(playerId, true);
        verify(player).setSendViewDistance(2);

        manager.setAfk(playerId, false);
        verify(player).setSendViewDistance(12);
    }

    @Test
    void afkBypass_usesNormalDistanceWhileAfk() {
        when(player.hasPermission("viewdistancecontrol.afkbypass")).thenReturn(true);
        when(player.getEffectivePermissions()).thenReturn(Set.of(
                perm("viewdistancecontrol.default.12")
        ));
        manager.setAfk(playerId, true);
        verify(player).setSendViewDistance(12);
    }

    @Test
    void removePlayer_clearsAfkState() {
        manager.setAfk(playerId, true);
        manager.removePlayer(playerId);

        // After removal, applying distance should use normal (not AFK) distance
        manager.applyViewDistance(player);
        verify(player, atLeast(1)).setSendViewDistance(10);
    }

    @Test
    void maxCap_limitsResolvedDistance() {
        when(player.getEffectivePermissions()).thenReturn(Set.of(
                perm("viewdistancecontrol.default.16"),
                perm("viewdistancecontrol.max.8")
        ));
        manager.applyViewDistance(player);
        verify(player).setSendViewDistance(8);
    }

    @Test
    void maxCap_doesNotRaiseDistance() {
        when(player.getEffectivePermissions()).thenReturn(Set.of(
                perm("viewdistancecontrol.default.4"),
                perm("viewdistancecontrol.max.12")
        ));
        manager.applyViewDistance(player);
        verify(player).setSendViewDistance(4);
    }

    @Test
    void multipleMaxCaps_highestWins() {
        when(player.getEffectivePermissions()).thenReturn(Set.of(
                perm("viewdistancecontrol.default.20"),
                perm("viewdistancecontrol.max.10"),
                perm("viewdistancecontrol.max.6")
        ));
        manager.applyViewDistance(player);
        verify(player).setSendViewDistance(10);
    }

    @Test
    void maxCap_appliesWhileAfk() {
        when(player.getEffectivePermissions()).thenReturn(Set.of(
                perm("viewdistancecontrol.afk.6"),
                perm("viewdistancecontrol.max.3")
        ));
        manager.setAfk(playerId, true);
        verify(player).setSendViewDistance(3);
    }

    @Test
    void maxCap_capsConfigDefaultWhenNoDefaultNode() {
        // config default is 10, cap is 5 — cap should apply even without a default.<N> node
        when(player.getEffectivePermissions()).thenReturn(Set.of(
                perm("viewdistancecontrol.max.5")
        ));
        manager.applyViewDistance(player);
        verify(player).setSendViewDistance(5);
    }

    @Test
    void getAfkState_afkWhenApplied() {
        manager.setAfk(playerId, true);
        assertEquals(AfkState.AFK, manager.getAfkState(playerId));
    }

    @Test
    void removePlayer_getAfkStateReturnsNormal() {
        manager.setAfk(playerId, true);
        manager.removePlayer(playerId);
        assertEquals(AfkState.NORMAL, manager.getAfkState(playerId));
    }

    @Test
    void serverViewDistance_clampsTooHighPermissionNode() {
        when(server.getViewDistance()).thenReturn(8);
        when(player.getEffectivePermissions()).thenReturn(Set.of(
                perm("viewdistancecontrol.default.16")
        ));
        when(player.getName()).thenReturn("TestPlayer");
        manager.applyViewDistance(player);
        verify(player).setSendViewDistance(8);
        verify(logger).warning(contains("TestPlayer"));
    }

    private PermissionAttachmentInfo perm(String node) {
        return new PermissionAttachmentInfo(player, node, null, true);
    }
}
