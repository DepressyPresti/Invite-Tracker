package dev.sean.invitetracker;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class DomainInviteTracker extends JavaPlugin implements Listener {

    private DiscordBot discord;
    private Storage storage;

    private final Map<String, DomainMapping> exactMap = new ConcurrentHashMap<>();
    private final List<DomainMapping> allMappings = new CopyOnWriteArrayList<>();

    private String currentToken = "";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadMappings();

        storage = new Storage(this);
        storage.load();

        currentToken = getConfig().getString("discord.botToken", "");
        startOrStopBotForToken(currentToken);

        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("DomainInviteTracker enabled.");
    }

    @Override
    public void onDisable() {
        if (discord != null) {
            discord.shutdown();
            discord = null;
        }
        if (storage != null) {
            storage.save();
        }
        getLogger().info("DomainInviteTracker disabled.");
    }

    private void startOrStopBotForToken(String token) {
        if (token == null || token.isBlank()) {
            if (discord != null) {
                discord.shutdown();
                discord = null;
            }
            getLogger().warning("discord.botToken is empty. The bot will not start.");
            return;
        }
        if (discord != null && currentToken.equals(token) && discord.isReady()) return;

        if (discord != null) {
            discord.shutdown();
            discord = null;
        }
        discord = new DiscordBot(this, token);
        discord.start();
    }

    private static String getAsString(FileConfiguration cfg, String path) {
        Object o = cfg.get(path);
        return o == null ? "" : String.valueOf(o);
    }

    private void loadMappings() {
        FileConfiguration cfg = getConfig();

        exactMap.clear();
        allMappings.clear();

        if (!cfg.isConfigurationSection("domains")) {
            cfg.set("domains.danasty.ashesofheaven.co.uk.channelId", "1403060253364588604");
            cfg.set("domains.danasty.ashesofheaven.co.uk.userId", "184058325246148609");

            cfg.set("domains.katvenly.ashesofheaven.co.uk.channelId", "1403060253364588604");
            cfg.set("domains.katvenly.ashesofheaven.co.uk.userId", "573732145349132288");

            saveConfig();
        }

        ConfigurationSection root = cfg.getConfigurationSection("domains");
        if (root == null) {
            getLogger().warning("No 'domains' section in config.");
            return;
        }

        List<String> fqdnKeys = new ArrayList<>();
        collectDomainLeaves(root, "", fqdnKeys);

        int loaded = 0;
        for (String fqdn : fqdnKeys) {
            String base = "domains." + fqdn + ".";
            String channelId = getAsString(cfg, base + "channelId").trim();
            String userId = getAsString(cfg, base + "userId").trim();

            if (channelId.isEmpty() || userId.isEmpty()) {
                getLogger().warning("Skipping domain '" + fqdn + "' due to missing channelId or userId.");
                continue;
            }

            String normalizedKey = fqdn.toLowerCase(Locale.ROOT);
            DomainMapping mapping = new DomainMapping(normalizedKey, channelId, userId);
            allMappings.add(mapping);
            exactMap.put(normalizedKey, mapping);
            loaded++;
        }

        if (loaded > 0) {
            StringBuilder sb = new StringBuilder("Loaded domain mappings: ");
            boolean first = true;
            for (DomainMapping m : allMappings) {
                if (!first) sb.append(", ");
                sb.append(m.domain());
                sb.append(" -> user ").append(m.userId())
                  .append(" / channel ").append(m.channelId());
                first = false;
            }
            getLogger().info(sb.toString());
        } else {
            getLogger().warning("No valid domain mappings loaded.");
        }
    }

    private void collectDomainLeaves(ConfigurationSection node, String prefix, List<String> out) {
        Set<String> keys = node.getKeys(false);
        boolean hasChannel = node.get("channelId") != null;
        boolean hasUser = node.get("userId") != null;

        if (hasChannel && hasUser) {
            out.add(prefix.endsWith(".") ? prefix.substring(0, prefix.length() - 1) : prefix);
            return;
        }

        for (String key : keys) {
            ConfigurationSection child = node.getConfigurationSection(key);
            if (child != null) {
                String newPrefix = prefix.isEmpty() ? key : prefix + "." + key;
                collectDomainLeaves(child, newPrefix, out);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;

        String raw = event.getHostname();
        if (raw == null || raw.isBlank()) return;

        String host = raw.toLowerCase(Locale.ROOT).trim();
        int colon = host.indexOf(':');
        if (colon > -1) host = host.substring(0, colon);

        DomainMapping mapping = exactMap.get(host);
        if (mapping == null) {
            return;
        }

        UUID uuid = event.getUniqueId();
        String name = event.getName();

        if (storage.markUniqueIfFirst(mapping.userId(), host, uuid)) {
            int total = storage.getInviteCount(mapping.userId(), host);

            int milestoneJustReached = milestoneNumberIfReached(total);
            int currentMilestone = milestoneIndexAtOrBelow(total);

            if (discord != null && discord.isReady()) {
                String lastId = storage.getLastMessageId(mapping.userId());
                if (lastId != null) {
                    discord.deleteMessage(mapping.channelId(), lastId);
                }

                String title = (milestoneJustReached > 0)
                        ? "<@" + mapping.userId() + "> has reached milestone " + milestoneJustReached
                        : "New invite via " + host;

                String contentPing = "<@" + mapping.userId() + ">";
                String newId = discord.sendJoinEmbed(
                        mapping.channelId(), contentPing, title, name, host,
                        total, currentMilestone, milestoneJustReached
                );
                if (newId != null) {
                    storage.setLastMessageId(mapping.userId(), newId);
                    storage.saveAsync();
                }
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("tracker")) return false;
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("invitetracker.reload")) {
                sender.sendMessage("§cYou do not have permission to do that.");
                return true;
            }
            long start = System.currentTimeMillis();
            reloadConfig();
            loadMappings();
            currentToken = getConfig().getString("discord.botToken", "");
            startOrStopBotForToken(currentToken);
            storage.save();
            long took = System.currentTimeMillis() - start;
            sender.sendMessage("§aDomainInviteTracker reloaded in " + took + "ms.");
            return true;
        }
        sender.sendMessage("§eUsage: /tracker reload");
        return true;
    }

    public static int milestoneNumberIfReached(int count) {
        int[] fixed = {3,5,7,10,12,15,17,20,22,25,27,30,32,35,37,40,42,45,47,50};
        for (int i = 0; i < fixed.length; i++) {
            if (count == fixed[i]) return i + 1;
        }
        if (count > 50 && count % 2 == 0) {
            return 20 + ((count - 50) / 2);
        }
        return 0;
    }

    public static int milestoneIndexAtOrBelow(int count) {
        int[] fixed = {3,5,7,10,12,15,17,20,22,25,27,30,32,35,37,40,42,45,47,50};
        int last = 0;
        for (int i = 0; i < fixed.length; i++) {
            if (count >= fixed[i]) last = i + 1;
        }
        if (count > 50) {
            last = 20 + ((count - 50) / 2);
        }
        return last;
    }

    public Storage storage() { return storage; }
}
