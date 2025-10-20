package ru.pointhome;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PointHome extends JavaPlugin implements Listener {

    private Map<UUID, Location> homes = new HashMap<>();
    private File homesFile;
    private FileConfiguration homesConfig;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Set<UUID> teleportingPlayers = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        initHomesFile();
        if (homesConfig == null) {
            getLogger().severe("Couldn't initialize homes.yml. The plugin will not work correctly.");
            setEnabled(false);
            return;
        }
        loadHomes();
        registerCommand("sethome", new SetHomeCommand());
        registerCommand("home", new HomeCommand());
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        saveHomes();
    }

    private void initHomesFile() {
        homesFile = new File(getDataFolder(), "homes.yml");
        if (!homesFile.exists()) {
            try {
                homesFile.getParentFile().mkdirs();
                homesFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Couldn't create homes.yml: " + e.getMessage());
                return;
            }
        }
        homesConfig = YamlConfiguration.loadConfiguration(homesFile);
    }

    private void loadHomes() {
        if (homesConfig == null || !homesConfig.contains("homes")) {
            return;
        }
        for (String key : homesConfig.getConfigurationSection("homes").getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid UUID in homes.yml: " + key);
                continue;
            }
            String path = "homes." + key;
            String worldName = homesConfig.getString(path + ".world");
            double x = homesConfig.getDouble(path + ".x");
            double y = homesConfig.getDouble(path + ".y");
            double z = homesConfig.getDouble(path + ".z");
            float yaw = (float) homesConfig.getDouble(path + ".yaw");
            float pitch = (float) homesConfig.getDouble(path + ".pitch");
            Location loc = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
            homes.put(uuid, loc);
        }
    }

    private void saveHomes() {
        if (homesConfig == null) {
            getLogger().severe("Couldn't save homes.yml: configuration is not initialized.");
            return;
        }
        homesConfig.set("homes", null);
        for (Map.Entry<UUID, Location> entry : homes.entrySet()) {
            UUID uuid = entry.getKey();
            Location loc = entry.getValue();
            String path = "homes." + uuid.toString();
            Player player = Bukkit.getPlayer(uuid);
            String playerName = player != null ? player.getName() : homesConfig.getString(path + ".player-name", "unknown");
            homesConfig.set(path + ".player-name", playerName);
            homesConfig.set(path + ".world", loc.getWorld().getName());
            homesConfig.set(path + ".x", loc.getX());
            homesConfig.set(path + ".y", loc.getY());
            homesConfig.set(path + ".z", loc.getZ());
            homesConfig.set(path + ".yaw", loc.getYaw());
            homesConfig.set(path + ".pitch", loc.getPitch());
        }
        try {
            homesConfig.save(homesFile);
        } catch (IOException e) {
            getLogger().severe("Couldn't save homes.yml: " + e.getMessage());
        }
    }

    private Component getMessage(String key, String... replacements) {
        String message = getConfig().getString("messages." + key, "<red>The message was not found: " + key);
        for (int i = 0; i < replacements.length; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        return miniMessage.deserialize(message);
    }

    private void playTeleportSound(Player player) {
        if (getConfig().getBoolean("teleport-sound.enabled", true)) {
            float volume = (float) getConfig().getDouble("teleport-sound.volume", 1.0);
            float pitch = (float) getConfig().getDouble("teleport-sound.pitch", 1.0);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, volume, pitch);
        }
    }

    private void spawnTeleportParticles(Player player) {
        if (getConfig().getBoolean("teleport-particles.enabled", true)) {
            int particleCount = getConfig().getInt("teleport-particles.count", 50);
            player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), particleCount, 0.5, 0.5, 0.5, 0.1);
        }
    }

    private void spawnDelayParticles(Player player) {
        if (getConfig().getBoolean("delay-particles.enabled", true)) {
            int particleCount = getConfig().getInt("delay-particles.count", 5);
            player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), particleCount, 0.5, 0.5, 0.5, 0.1);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (teleportingPlayers.contains(uuid)) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (to == null || from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                teleportingPlayers.remove(uuid);
                player.sendMessage(getMessage("teleport-delay-cancelled"));
            }
        }
    }

    class SetHomeCommand implements BasicCommand {
        @Override
        public void execute(CommandSourceStack source, String[] args) {
            if (!(source.getSender() instanceof Player player)) {
                source.getSender().sendMessage(getMessage("player-only"));
                return;
            }
            homes.put(player.getUniqueId(), player.getLocation());
            player.sendMessage(getMessage("set-home"));
            saveHomes();
        }

        @Override
        public boolean canUse(org.bukkit.command.CommandSender sender) {
            return sender instanceof Player;
        }
    }

    class HomeCommand implements BasicCommand {
        @Override
        public void execute(CommandSourceStack source, String[] args) {
            if (!(source.getSender() instanceof Player player)) {
                source.getSender().sendMessage(getMessage("player-only"));
                return;
            }
            Location home = homes.get(player.getUniqueId());
            if (home == null) {
                player.sendMessage(getMessage("home-not-set"));
                return;
            }
            boolean delayEnabled = getConfig().getBoolean("teleport-delay.enabled", true);
            int delaySeconds = getConfig().getInt("teleport-delay.seconds", 3);
            if (delayEnabled && delaySeconds > 0) {
                UUID uuid = player.getUniqueId();
                teleportingPlayers.add(uuid);
                player.sendMessage(getMessage("teleport-delay-start", "<seconds>", String.valueOf(delaySeconds)));
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (!teleportingPlayers.contains(uuid)) {
                            cancel();
                            return;
                        }
                        if (ticks >= delaySeconds * 20) {
                            teleportingPlayers.remove(uuid);
                            player.teleport(home);
                            playTeleportSound(player);
                            spawnTeleportParticles(player);
                            player.sendMessage(getMessage("home-teleport"));
                            cancel();
                        } else {
                            spawnDelayParticles(player);
                            ticks++;
                        }
                    }
                }.runTaskTimer(PointHome.this, 0L, 1L);
            } else {
                player.teleport(home);
                playTeleportSound(player);
                spawnTeleportParticles(player);
                player.sendMessage(getMessage("home-teleport"));
            }
        }

        @Override
        public boolean canUse(org.bukkit.command.CommandSender sender) {
            return sender instanceof Player;
        }
    }
}