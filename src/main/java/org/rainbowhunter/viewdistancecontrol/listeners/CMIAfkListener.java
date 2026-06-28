package org.rainbowhunter.viewdistancecontrol.listeners;

import com.Zrips.CMI.events.CMIAfkEnterEvent;
import com.Zrips.CMI.events.CMIAfkLeaveEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.rainbowhunter.viewdistancecontrol.ConfigManager;
import org.rainbowhunter.viewdistancecontrol.ViewDistanceManager;

import java.util.logging.Logger;

public class CmiAfkListener implements Listener {

    private final ViewDistanceManager viewDistanceManager;
    private final ConfigManager config;
    private final Logger logger;

    public CmiAfkListener(JavaPlugin plugin, ConfigManager config, ViewDistanceManager viewDistanceManager) {
        this.viewDistanceManager = viewDistanceManager;
        this.config = config;
        this.logger = plugin.getLogger();
    }

    @EventHandler
    public void onCmiAfkEnter(CMIAfkEnterEvent event) {
        Player player = event.getPlayer();

        if (player == null) {
            return;
        }

        if (config.isDebug()) {
            logger.info("[DEBUG] CmiAfkListener enter triggered for "
                    + player.getName()
                    + ", afk=true");
        }

        viewDistanceManager.handleAfkChange(
                player.getUniqueId(),
                true
        );
    }

    @EventHandler
    public void onCmiAfkLeave(CMIAfkLeaveEvent event) {
        Player player = event.getPlayer();

        if (player == null) {
            return;
        }

        if (config.isDebug()) {
            logger.info("[DEBUG] CmiAfkListener leave triggered for "
                    + player.getName()
                    + ", afk=false");
        }

        viewDistanceManager.handleAfkChange(
                player.getUniqueId(),
                false
        );
    }
}