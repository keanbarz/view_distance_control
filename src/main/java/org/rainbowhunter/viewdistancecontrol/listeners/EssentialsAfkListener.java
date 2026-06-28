package org.rainbowhunter.viewdistancecontrol.listeners;

import net.ess3.api.events.AfkStatusChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.rainbowhunter.viewdistancecontrol.ConfigManager;
import org.rainbowhunter.viewdistancecontrol.ViewDistanceManager;

import java.util.logging.Logger;

public class AfkListener implements Listener {

    private final ViewDistanceManager viewDistanceManager;
    private final ConfigManager config;
    private final Logger logger;

    public AfkListener(JavaPlugin plugin, ConfigManager config, ViewDistanceManager viewDistanceManager) {
        this.viewDistanceManager = viewDistanceManager;
        this.config = config;
        this.logger = plugin.getLogger();
    }

    @EventHandler
    public void onAfkStatusChange(AfkStatusChangeEvent event) {
        if (config.isDebug()) logger.info("[DEBUG] AfkListener.onAfkStatusChange triggered for " + event.getAffected().getName() + ", afk=" + event.getValue());
        viewDistanceManager.handleAfkChange(event.getAffected().getUUID(), event.getValue());
    }
}
