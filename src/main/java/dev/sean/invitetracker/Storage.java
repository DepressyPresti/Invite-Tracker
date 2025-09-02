package dev.sean.invitetracker;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class Storage {

    private final JavaPlugin plugin;
    private final File file;
    private FileConfiguration data;

    public Storage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load() {
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        data = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save data.yml: " + e.getMessage());
        }
    }

    public void saveAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::save);
    }

    public synchronized boolean markUniqueIfFirst(String userId, String domain, UUID uuid) {
        String base = "users." + userId + ".domains." + domain + ".uuids";
        List<String> list = data.getStringList(base);
        String s = uuid.toString();
        if (list.contains(s)) return false;
        list.add(s);
        data.set(base, list);
        data.set("users." + userId + ".domains." + domain + ".count", list.size());
        return true;
    }

    public synchronized int getInviteCount(String userId, String domain) {
        return data.getInt("users." + userId + ".domains." + domain + ".count", 0);
    }

    public synchronized String getLastMessageId(String userId) {
        return data.getString("users." + userId + ".lastMessageId");
    }

    public synchronized void setLastMessageId(String userId, String messageId) {
        data.set("users." + userId + ".lastMessageId", messageId);
    }
}
