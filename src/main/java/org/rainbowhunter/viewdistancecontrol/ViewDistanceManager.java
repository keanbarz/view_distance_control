package org.rainbowhunter.viewdistancecontrol;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ViewDistanceManager {

    private static final String DEFAULT_PREFIX = "viewdistancecontrol.default.";
    private static final String AFK_PREFIX = "viewdistancecontrol.afk.";
    private static final String MAX_PREFIX = "viewdistancecontrol.max.";

    private final JavaPlugin plugin;
    private final ConfigManager config;
    private final Set<UUID> afkPlayers = new HashSet<>();
    private final Map<UUID, BukkitTask> pendingAfkTasks = new ConcurrentHashMap<>();

    public ViewDistanceManager(JavaPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void applyViewDistance(Player player) {
        boolean isAfk = afkPlayers.contains(player.getUniqueId())
                && !player.hasPermission("viewdistancecontrol.afkbypass");
        int distance;

        if (isAfk && config.isAfkEnabled()) {
            distance = getPermissionDistance(player, AFK_PREFIX);
            if (distance < 0) distance = config.getDefaultAfkViewDistance();
        } else {
            distance = getPermissionDistance(player, DEFAULT_PREFIX);
            if (distance < 0) distance = config.getDefaultViewDistance();
        }

        int cap = getCapDistance(player);
        if (cap >= 0) distance = Math.min(distance, cap);

        // The default values are clamped at the config manager. if it is still higher than
        // the cap, it must come from a permission node.
        int serverMax = Bukkit.getServer().getViewDistance();
        if (distance > serverMax) {
            plugin.getLogger().warning("Player " + player.getName() + " has a permission node granting view distance " + distance + " which exceeds server view-distance (" + serverMax + "). Clamping to " + serverMax + ".");
            distance = serverMax;
        }

        int previous = player.getSendViewDistance();

        if (previous != distance) {
            player.setSendViewDistance(distance);
            if (config.isConsoleLog()) {
                plugin.getLogger().info("View distance of " + player.getName() + " has changed to " + distance + ".");
            }
            if (config.isNotifyPlayer()) {
                notifyPlayer(player, distance);
            }
        }
    }

    public void applyAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyViewDistance(player);
        }
    }

    public void handleAfkChange(UUID uuid, boolean isAfk) {
        if (isAfk) {
            BukkitTask old = pendingAfkTasks.remove(uuid);
            if (old != null) old.cancel();
            int delayTicks = config.getAfkDelaySeconds() * 20;
            if (delayTicks <= 0) {
                setAfk(uuid, true);
            } else {
                BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    pendingAfkTasks.remove(uuid);
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) setAfk(uuid, true);
                }, delayTicks);
                pendingAfkTasks.put(uuid, task);
            }
        } else {
            BukkitTask pending = pendingAfkTasks.remove(uuid);
            if (pending != null) {
                pending.cancel();
            } else {
                setAfk(uuid, false);
            }
        }
    }

    public void setAfk(UUID uuid, boolean afk) {
        if (afk) {
            afkPlayers.add(uuid);
        } else {
            afkPlayers.remove(uuid);
        }
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            applyViewDistance(player);
        }
    }

    public void removePlayer(UUID uuid) {
        afkPlayers.remove(uuid);
        BukkitTask pending = pendingAfkTasks.remove(uuid);
        if (pending != null) pending.cancel();
    }

    public AfkState getAfkState(UUID uuid) {
        if (afkPlayers.contains(uuid)) return AfkState.AFK;
        if (pendingAfkTasks.containsKey(uuid)) return AfkState.PENDING;
        return AfkState.NORMAL;
    }

    public void cleanup() {
        pendingAfkTasks.values().forEach(BukkitTask::cancel);
        pendingAfkTasks.clear();
    }

    private void notifyPlayer(Player player, int distance) {
        String raw = config.getNotifyMessage()
                .replace("%viewdistancecontrol_distance%", String.valueOf(distance));
        raw = PlaceholderAPI.setPlaceholders(player, raw);
        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(raw));
    }

    private int getPermissionDistance(Player player, String prefix) {
        int highest = -1;
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (!info.getValue()) continue;
            String node = info.getPermission();
            if (!node.startsWith(prefix)) continue;
            try {
                int val = Integer.parseInt(node.substring(prefix.length()));
                if (val > highest) highest = val;
            } catch (NumberFormatException ignored) {
            }
        }
        return highest;
    }

    // Returns the highest max.<N> value, or -1 if no cap is set.
    private int getCapDistance(Player player) {
        int highest = -1;
        String prefix = "viewdistancecontrol.max.";

        for (PermissionAttachmentInfo permissionInfo : player.getEffectivePermissions()) {
            if (!permissionInfo.getValue()) {
                continue;
            }

            String permission = permissionInfo.getPermission();

            if (!permission.startsWith(prefix)) {
                continue;
            }

            try {
                int value = Integer.parseInt(permission.substring(prefix.length()));

                if (value > highest) {
                    highest = value;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return highest;
    }
}
