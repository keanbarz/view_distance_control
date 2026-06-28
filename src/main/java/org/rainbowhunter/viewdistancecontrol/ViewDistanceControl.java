package org.rainbowhunter.viewdistancecontrol;

import net.luckperms.api.LuckPerms;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.rainbowhunter.viewdistancecontrol.commands.VdcCommand;
import org.rainbowhunter.viewdistancecontrol.listeners.CMIAfkListener;
import org.rainbowhunter.viewdistancecontrol.listeners.EssentialsAfkListener;
import org.rainbowhunter.viewdistancecontrol.listeners.LuckPermsListener;
import org.rainbowhunter.viewdistancecontrol.listeners.PlayerListener;

public class ViewDistanceControl extends JavaPlugin {

    private LuckPermsListener luckPermsListener;
    private ViewDistanceManager viewDistanceManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ConfigManager configManager = new ConfigManager(this);

        RegisteredServiceProvider<LuckPerms> luckPermsProvider =
                getServer().getServicesManager().getRegistration(LuckPerms.class);
        if (luckPermsProvider == null) {
            getLogger().severe("LuckPerms not found. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        LuckPerms luckPerms = luckPermsProvider.getProvider();

        viewDistanceManager = new ViewDistanceManager(this, configManager);

        getServer().getPluginManager().registerEvents(new PlayerListener(this, configManager, viewDistanceManager), this);
        registerAfkListener(configManager);
        luckPermsListener = new LuckPermsListener(this, configManager, luckPerms, viewDistanceManager);
        luckPermsListener.register();

        VdcCommand cmd = new VdcCommand(configManager, viewDistanceManager);
        PluginCommand vdc = getCommand("vdc");
        if (vdc != null) {
            vdc.setExecutor(cmd);
            vdc.setTabCompleter(cmd);
        }

        new VdcPlaceholderExpansion(this).register();
    }

    private void registerAfkListener(ConfigManager configManager) {
        boolean cmiEnabled = getServer().getPluginManager().isPluginEnabled("CMI");
        boolean essentialsEnabled = getServer().getPluginManager().isPluginEnabled("Essentials");

        if (cmiEnabled) {
            getServer().getPluginManager().registerEvents(
                    new CMIAfkListener(this, configManager, viewDistanceManager),
                    this
            );

            getLogger().info("Using CMI AFK integration.");
            return;
        }

        if (essentialsEnabled) {
            getServer().getPluginManager().registerEvents(
                    new EssentialsAfkListener(this, configManager, viewDistanceManager),
                    this
            );

            getLogger().info("Using EssentialsX AFK integration.");
            return;
        }

        getLogger().warning("Neither CMI nor EssentialsX was found. AFK view distance changes will not work.");
    }

    @Override
    public void onDisable() {
        if (luckPermsListener != null) luckPermsListener.unregister();
        if (viewDistanceManager != null) viewDistanceManager.cleanup();
    }
}
