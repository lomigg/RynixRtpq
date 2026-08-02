package com.rynix.rtpq.config;

import com.rynix.rtpq.RynixRtpqPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

/**
 * ConfigManager - Supports McPlugin format + RynixRtpq Duo extension
 * Plugin: RynixRtpq v2.0 | Author: RinZz | Folia Supported
 */
public class ConfigManager {

    private final RynixRtpqPlugin plugin;
    private FileConfiguration config;

    // Queue
    private String queueHashCommand;
    private List<String> queueHashAliases;
    private int queueInviteTime;
    private int queueInviteCooldown;
    private String worldDefault;
    private boolean damagedCancel;
    private int delayBetweenTeleports;
    private int maxQueueSize;

    // Duo
    private boolean duoEnabled;
    private int duoSize;
    private boolean duoSameLocation;
    private int duoSpreadDistance;
    private int duoSoloAfterSeconds;

    // Worlds - from list-world
    private final Map<String, WorldConfig> worldConfigs = new HashMap<>();

    // Menu configs
    private String menuTitle;
    private int menuSize;
    private boolean menuFillerEnable;
    private String menuFillerMaterial;
    private final Map<String, MenuItem> menuItems = new HashMap<>();

    private String worldMenuTitle;
    private int worldMenuSize;
    private boolean worldMenuFillerEnable;
    private String worldMenuFillerMaterial;

    private String inviteMenuTitle;
    private int inviteMenuSize;
    private boolean inviteMenuFillerEnable;
    private String inviteMenuFillerMaterial;
    private MenuItem inviteNextItem;
    private MenuItem invitePrevItem;
    private MenuItem inviteCloseItem;

    // BossBar
    private boolean bossBarEnable;
    private String bossBarTitle;
    private String bossBarColor;
    private String bossBarStyle;

    // Title
    private boolean titleEnable;
    private String titleTeleport;
    private String subtitleTeleport;
    private boolean titleTime;
    private String titleTeleported;
    private String subtitleTeleported;

    // ActionBar
    private boolean actionBarEnable;
    private String actionBarMessage;

    // Broadcast
    private String broadcastJoin;
    private String broadcastFoundTitle;
    private String broadcastFoundMessage;

    // Messages
    private final Map<String, String> messages = new HashMap<>();

    // Sound
    private final Map<String, SoundConfig> sounds = new HashMap<>();

    // Other
    private boolean cooldownEnabled;
    private int cooldownTime;
    private String prefix;
    private boolean invulnEnabled;
    private int invulnSeconds;

    public ConfigManager(RynixRtpqPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        // Queue
        queueHashCommand = config.getString("queue.hash", "/rtpqueue");
        queueHashAliases = config.getStringList("queue.hash-aliases");
        if (queueHashAliases.isEmpty()) queueHashAliases = Arrays.asList("/rtpq", "/mchashqueue");
        queueInviteTime = config.getInt("queue.invite-time", 60);
        queueInviteCooldown = config.getInt("queue.invite-cooldown", 60);
        worldDefault = config.getString("queue.world-default", config.getString("rtp.default-world", "world"));
        damagedCancel = config.getBoolean("queue.damaged-cancel", true);
        delayBetweenTeleports = config.getInt("queue.delay-between-teleports", 3);
        maxQueueSize = config.getInt("queue.max-queue-size", 100);

        // Duo extension
        duoEnabled = config.getBoolean("duo.enabled", true);
        duoSize = config.getInt("duo.size", config.getInt("list-world.world.require", 2));
        duoSameLocation = config.getBoolean("duo.same-location", true);
        duoSpreadDistance = config.getInt("duo.spread-distance", 2);
        duoSoloAfterSeconds = config.getInt("duo.solo-after-seconds", 60);

        // list-world
        worldConfigs.clear();
        ConfigurationSection listWorld = config.getConfigurationSection("list-world");
        if (listWorld != null) {
            for (String wName : listWorld.getKeys(false)) {
                ConfigurationSection ws = listWorld.getConfigurationSection(wName);
                if (ws == null) continue;
                int require = ws.getInt("require", duoSize);
                int interval = ws.getInt("interval", delayBetweenTeleports);
                boolean safe = ws.getBoolean("safe", true);
                ConfigurationSection center = ws.getConfigurationSection("center");
                int cx = 0, cz = 0;
                if (center != null) {
                    cx = center.getInt("x", 0);
                    cz = center.getInt("z", 0);
                }
                int radius = ws.getInt("radius", 500);
                int minRadius = ws.getInt("min-radius", 50);
                int attempts = ws.getInt("attempts", 20);
                int minY = ws.getInt("min-y", 62);
                int maxY = ws.getInt("max-y", 200);
                List<String> unsafe = ws.getStringList("unsafe-block");
                Set<String> unsafeSet = new HashSet<>();
                for (String s : unsafe) unsafeSet.add(s.toUpperCase());

                WorldConfig wc = new WorldConfig(wName, true, cx, cz, minRadius, radius, minY, maxY, attempts, require, interval, safe, unsafeSet);
                worldConfigs.put(wName.toLowerCase(), wc);
            }
        }
        // Fallback to old rtp.worlds if list-world empty
        if (worldConfigs.isEmpty()) {
            ConfigurationSection oldWorlds = config.getConfigurationSection("rtp.worlds");
            if (oldWorlds != null) {
                for (String wName : oldWorlds.getKeys(false)) {
                    ConfigurationSection ws = oldWorlds.getConfigurationSection(wName);
                    if (ws == null) continue;
                    WorldConfig wc = new WorldConfig(
                            wName,
                            ws.getBoolean("enabled", true),
                            ws.getInt("center-x", 0),
                            ws.getInt("center-z", 0),
                            ws.getInt("min-radius", 50),
                            ws.getInt("max-radius", 500),
                            ws.getInt("min-y", 62),
                            ws.getInt("max-y", 200),
                            ws.getInt("max-attempts", 20),
                            duoSize,
                            delayBetweenTeleports,
                            true,
                            Set.of("WATER","LAVA")
                    );
                    worldConfigs.put(wName.toLowerCase(), wc);
                }
            }
        }

        // Menu
        menuTitle = config.getString("menu.title", config.getString("gui.title", "&8● &bRynixRtpq &8| &dDUO Queue &8●"));
        menuSize = config.getInt("menu.size", config.getInt("gui.size", 27));
        menuFillerEnable = config.getBoolean("menu.filler.enable", config.getBoolean("gui.filler-enabled", true));
        menuFillerMaterial = config.getString("menu.filler.material", config.getString("gui.filler-material", "BLACK_STAINED_GLASS_PANE"));

        menuItems.clear();
        ConfigurationSection menuSec = config.getConfigurationSection("menu");
        if (menuSec != null) {
            for (String key : menuSec.getKeys(false)) {
                if (key.equals("title") || key.equals("size") || key.equals("filler")) continue;
                ConfigurationSection itemSec = menuSec.getConfigurationSection(key);
                if (itemSec == null) continue;
                MenuItem item = parseMenuItem(key, itemSec);
                menuItems.put(key.toLowerCase(), item);
            }
        }

        // World menu
        worldMenuTitle = config.getString("world-menu.title", "&8● &aWorld Selector &8●");
        worldMenuSize = config.getInt("world-menu.size", 27);
        worldMenuFillerEnable = config.getBoolean("world-menu.filler.enable", true);
        worldMenuFillerMaterial = config.getString("world-menu.filler.material", "BLACK_STAINED_GLASS_PANE");

        // Invite menu
        inviteMenuTitle = config.getString("invite-menu.title", "&8● &eInvite Player &8●");
        inviteMenuSize = config.getInt("invite-menu.size", 54);
        inviteMenuFillerEnable = config.getBoolean("invite-menu.filler.enable", true);
        inviteMenuFillerMaterial = config.getString("invite-menu.filler.material", "BLACK_STAINED_GLASS_PANE");

        ConfigurationSection invSec = config.getConfigurationSection("invite-menu");
        if (invSec != null) {
            ConfigurationSection nextSec = invSec.getConfigurationSection("next");
            if (nextSec != null) inviteNextItem = parseMenuItem("next", nextSec);
            ConfigurationSection prevSec = invSec.getConfigurationSection("previous");
            if (prevSec != null) invitePrevItem = parseMenuItem("previous", prevSec);
            ConfigurationSection closeSec = invSec.getConfigurationSection("close");
            if (closeSec != null) inviteCloseItem = parseMenuItem("close", closeSec);
        }

        // BossBar
        bossBarEnable = config.getBoolean("boss-bar.enable", true);
        bossBarTitle = config.getString("boss-bar.title", "&bRynixRtpq &8| &dDUO &7- &f{count} chờ");
        bossBarColor = config.getString("boss-bar.color", "RED");
        bossBarStyle = config.getString("boss-bar.style", "SOLID");

        // Title
        titleEnable = config.getBoolean("title.enable", true);
        titleTeleport = config.getString("title.title-teleport", "&b&lMatch &dDuo");
        subtitleTeleport = config.getString("title.subtitle-teleport", "");
        titleTime = config.getBoolean("title.time", true);
        titleTeleported = config.getString("title.title-teleported", "&a&lDUO!");
        subtitleTeleported = config.getString("title.subtitle-teleported", "");

        // ActionBar
        actionBarEnable = config.getBoolean("action-bar.enable", true);
        actionBarMessage = config.getString("action-bar.message", "&eTeleport in &f${interval}");

        // Broadcast
        broadcastJoin = config.getString("boardcast.join", "&e{player} &ajoined &b&l{world} &aqueue");
        broadcastFoundTitle = config.getString("boardcast.found.title", "&b&lFound the match in {world}");
        broadcastFoundMessage = config.getString("boardcast.found.message", "&e{player_list}");

        // Messages
        messages.clear();
        ConfigurationSection msgSec = config.getConfigurationSection("message");
        if (msgSec == null) msgSec = config.getConfigurationSection("messages");
        if (msgSec != null) {
            for (String key : msgSec.getKeys(false)) {
                Object val = msgSec.get(key);
                if (val instanceof String) messages.put(key.toLowerCase(), (String) val);
                else if (val instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> list = (List<String>) val;
                    messages.put(key.toLowerCase(), String.join("\n", list));
                }
            }
        }

        // Sound
        sounds.clear();
        ConfigurationSection soundSec = config.getConfigurationSection("sound");
        if (soundSec != null) {
            for (String key : soundSec.getKeys(false)) {
                ConfigurationSection sSec = soundSec.getConfigurationSection(key);
                if (sSec == null) continue;
                String name = sSec.getString("name", "UI_BUTTON_CLICK");
                float volume = (float) sSec.getDouble("volume", 1.0);
                float pitch = (float) sSec.getDouble("pitch", 1.0);
                sounds.put(key.toLowerCase(), new SoundConfig(name, volume, pitch));
            }
        }

        // Other
        prefix = config.getString("settings.prefix", "&8[&bRynixRtpq&8] &f");
        cooldownEnabled = config.getBoolean("cooldown.enabled", true);
        cooldownTime = config.getInt("cooldown.time-seconds", 300);
        invulnEnabled = config.getBoolean("rtp.invulnerability.enabled", true);
        invulnSeconds = config.getInt("rtp.invulnerability.seconds", 5);
    }

    private MenuItem parseMenuItem(String key, ConfigurationSection sec) {
        String material = sec.getString("material", "STONE");
        String display = sec.getString("display_name", "&f" + key);
        List<String> lore = sec.getStringList("lore");
        int slot = sec.getInt("slot", 0);
        // support for world-menu extra
        String select = sec.getString("select", null);
        return new MenuItem(key, material, display, lore, slot, select);
    }

    // Getters
    public String getQueueHashCommand() { return queueHashCommand.replace("/", ""); }
    public String getQueueHashCommandRaw() { return queueHashCommand; }
    public List<String> getQueueHashAliases() { return queueHashAliases; }
    public int getQueueInviteTime() { return queueInviteTime; }
    public int getQueueInviteCooldown() { return queueInviteCooldown; }
    public String getWorldDefault() { return worldDefault; }
    public boolean isDamagedCancel() { return damagedCancel; }
    public int getQueueDelay() { return delayBetweenTeleports; }
    public int getMaxQueueSize() { return maxQueueSize; }

    public boolean isDuoEnabled() { return duoEnabled; }
    public int getDuoSize() { return duoSize; }
    public boolean isDuoSameLocation() { return duoSameLocation; }
    public int getDuoSpreadDistance() { return duoSpreadDistance; }
    public int getDuoSoloAfterSeconds() { return 0; } // disable solo timeout for McPlugin strict duo
    public int getQueueDelayRaw() { return delayBetweenTeleports; }

    public Map<String, WorldConfig> getWorldConfigs() { return worldConfigs; }
    public WorldConfig getWorldConfig(String world) {
        if (world == null) return null;
        return worldConfigs.get(world.toLowerCase());
    }

    public String getMenuTitle() { return menuTitle; }
    public int getMenuSize() { return menuSize; }
    public boolean isMenuFillerEnable() { return menuFillerEnable; }
    public String getMenuFillerMaterial() { return menuFillerMaterial; }
    public Map<String, MenuItem> getMenuItems() { return menuItems; }
    public MenuItem getMenuItem(String key) { return menuItems.get(key.toLowerCase()); }

    public String getWorldMenuTitle() { return worldMenuTitle; }
    public int getWorldMenuSize() { return worldMenuSize; }
    public boolean isWorldMenuFillerEnable() { return worldMenuFillerEnable; }
    public String getWorldMenuFillerMaterial() { return worldMenuFillerMaterial; }

    public String getInviteMenuTitle() { return inviteMenuTitle; }
    public int getInviteMenuSize() { return inviteMenuSize; }
    public boolean isInviteMenuFillerEnable() { return inviteMenuFillerEnable; }
    public String getInviteMenuFillerMaterial() { return inviteMenuFillerMaterial; }
    public MenuItem getInviteNextItem() { return inviteNextItem; }
    public MenuItem getInvitePrevItem() { return invitePrevItem; }
    public MenuItem getInviteCloseItem() { return inviteCloseItem; }

    public boolean isBossBarEnable() { return bossBarEnable; }
    public String getBossBarTitle() { return bossBarTitle; }
    public String getBossBarColor() { return bossBarColor; }
    public String getBossBarStyle() { return bossBarStyle; }

    public boolean isTitleEnable() { return titleEnable; }
    public String getTitleTeleport() { return titleTeleport; }
    public String getSubtitleTeleport() { return subtitleTeleport; }
    public boolean isTitleTime() { return titleTime; }
    public String getTitleTeleported() { return titleTeleported; }
    public String getSubtitleTeleported() { return subtitleTeleported; }

    public boolean isActionBarEnable() { return actionBarEnable; }
    public String getActionBarMessage() { return actionBarMessage; }

    public String getBroadcastJoin() { return broadcastJoin; }
    public String getBroadcastFoundTitle() { return broadcastFoundTitle; }
    public String getBroadcastFoundMessage() { return broadcastFoundMessage; }

    public String getMessage(String key) {
        return messages.getOrDefault(key.toLowerCase(), "&cMessage not found: " + key);
    }
    public String getMessage(String key, Map<String, String> placeholders) {
        String msg = getMessage(key);
        if (placeholders != null) {
            for (var e : placeholders.entrySet()) {
                msg = msg.replace("{" + e.getKey() + "}", e.getValue())
                        .replace("${" + e.getKey() + "}", e.getValue());
            }
        }
        return msg;
    }
    public List<String> getMessageList(String path) {
        return config.getStringList("message." + path);
    }

    public SoundConfig getSound(String key) {
        return sounds.get(key.toLowerCase());
    }
    public Map<String, SoundConfig> getSounds() { return sounds; }

    public String getPrefix() { return prefix; }
    public boolean isCooldownEnabled() { return cooldownEnabled; }
    public int getCooldownTime() { return cooldownTime; }
    public boolean isInvulnerabilityEnabled() { return invulnEnabled; }
    public int getInvulnerabilitySeconds() { return invulnSeconds; }

    // Legacy getters for old code compatibility
    public String getGuiTitle() { return menuTitle; }
    public int getGuiSize() { return menuSize; }
    public boolean isGuiSoundEnabled() { return true; }
    public boolean isFillerEnabled() { return menuFillerEnable; }
    public String getFillerMaterial() { return menuFillerMaterial; }
    public boolean isActionBarEnabled() { return actionBarEnable; }
    public boolean isBossBarEnabled() { return bossBarEnable; }
    public boolean isRemoveOnQuit() { return true; }
    public boolean isPrioritySystem() { return true; }
    public boolean isDebug() { return config.getBoolean("settings.debug", false); }
    public boolean avoidLava() { return true; }
    public boolean avoidWater() { return true; }
    public boolean avoidFire() { return true; }
    public boolean avoidCactus() { return true; }
    public boolean needSolidGround() { return true; }
    public boolean needTwoAirBlocks() { return true; }
    public Set<String> getBlacklistedBlocks() { return Set.of("WATER","LAVA"); }
    public String getDefaultWorld() { return worldDefault; }
    public long getGuiRefreshTicks() { return 20L; }
    public String getMessageRaw(String key) { return getMessage(key); }

    public boolean isDuoRequireSameWorld() { return false; }
    public boolean isDuoNotifyPartner() { return true; }

    public static class WorldConfig {
        public final String name;
        public final boolean enabled;
        public final int centerX;
        public final int centerZ;
        public final int minRadius;
        public final int maxRadius;
        public final int minY;
        public final int maxY;
        public final int maxAttempts;
        public final int require;
        public final int interval;
        public final boolean safe;
        public final Set<String> unsafeBlocks;

        public WorldConfig(String name, boolean enabled, int centerX, int centerZ, int minRadius, int maxRadius, int minY, int maxY, int maxAttempts, int require, int interval, boolean safe, Set<String> unsafe) {
            this.name = name;
            this.enabled = enabled;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.minRadius = minRadius;
            this.maxRadius = maxRadius;
            this.minY = minY;
            this.maxY = maxY;
            this.maxAttempts = maxAttempts;
            this.require = require;
            this.interval = interval;
            this.safe = safe;
            this.unsafeBlocks = unsafe;
        }
    }

    public static class MenuItem {
        public final String key;
        public final String material;
        public final String displayName;
        public final List<String> lore;
        public final int slot;
        public final String select; // for world-menu

        public MenuItem(String key, String material, String displayName, List<String> lore, int slot, String select) {
            this.key = key;
            this.material = material;
            this.displayName = displayName;
            this.lore = lore;
            this.slot = slot;
            this.select = select;
        }
    }

    public static class SoundConfig {
        public final String name;
        public final float volume;
        public final float pitch;
        public SoundConfig(String name, float volume, float pitch) {
            this.name = name;
            this.volume = volume;
            this.pitch = pitch;
        }
    }
}
