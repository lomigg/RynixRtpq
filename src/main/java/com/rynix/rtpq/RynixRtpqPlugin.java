package com.rynix.rtpq;

import com.rynix.rtpq.commands.RTPCommand;
import com.rynix.rtpq.commands.RynixRtpqCommand;
import com.rynix.rtpq.config.ConfigManager;
import com.rynix.rtpq.gui.GUIListener;
import com.rynix.rtpq.gui.InviteMenuGUI;
import com.rynix.rtpq.gui.RtpqGUI;
import com.rynix.rtpq.gui.WorldMenuGUI;
import com.rynix.rtpq.invite.InviteManager;
import com.rynix.rtpq.listeners.PlayerListener;
import com.rynix.rtpq.queue.QueueManager;
import com.rynix.rtpq.rtp.RTPService;
import com.rynix.rtpq.scheduler.FoliaScheduler;
import com.rynix.rtpq.scheduler.PaperScheduler;
import com.rynix.rtpq.scheduler.SchedulerAdapter;
import com.rynix.rtpq.utils.ChatUtil;
import com.rynix.rtpq.utils.CompatibilityUtil;
import com.rynix.rtpq.utils.CooldownManager;
import com.rynix.rtpq.utils.LoggerUtil;
import com.rynix.rtpq.utils.SoundManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main Plugin Class - RynixRtpq v2.0 Duo - McPlugin format + Folia
 * Commands: /rtpq and /rtpqueue have GUI
 * 2 Players teleport to same location
 * Author: RinZz - Rynix Studio
 */
public class RynixRtpqPlugin extends JavaPlugin {

    private static RynixRtpqPlugin instance;

    private ConfigManager configManager;
    private QueueManager queueManager;
    private RTPService rtpService;
    private CooldownManager cooldownManager;
    private RtpqGUI gui;
    private WorldMenuGUI worldMenuGUI;
    private InviteMenuGUI inviteMenuGUI;
    private SchedulerAdapter scheduler;
    private InviteManager inviteManager;
    private SoundManager soundManager;

    private boolean isFolia = false;

    @Override
    public void onLoad() {
        instance = this;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (ClassNotFoundException e) {
            isFolia = false;
        }
    }

    @Override
    public void onEnable() {
        LoggerUtil.init(getLogger());
        LoggerUtil.logStartup();
        CompatibilityUtil.logCompatibility();
        LoggerUtil.info("§d[DUO MODE + McPlugin Format + Java 21-25 + All MC Versions] 2 players same location | Commands /rtpq & /rtpqueue have GUI | Author RinZz");

        if (isFolia) {
            scheduler = new FoliaScheduler();
            LoggerUtil.info("§aFolia detected! Using FoliaScheduler | RynixRtpq v2.0 Duo by RinZz");
        } else {
            scheduler = new PaperScheduler();
            LoggerUtil.info("§aPaper/Spigot detected! Using PaperScheduler | RynixRtpq v2.0 Duo by RinZz");
        }

        configManager = new ConfigManager(this);
        configManager.load();
        ChatUtil.setPrefix(configManager.getPrefix());

        cooldownManager = new CooldownManager();
        queueManager = new QueueManager(this);
        rtpService = new RTPService(this);
        gui = new RtpqGUI(this);
        worldMenuGUI = new WorldMenuGUI(this);
        inviteMenuGUI = new InviteMenuGUI(this);
        inviteManager = new InviteManager(this);
        soundManager = new SoundManager(this);

        // Commands: /rtpq and /rtpqueue both have GUI - as requested
        RTPCommand rtpqCmd = new RTPCommand(this);
        if (getCommand("rtpq") != null) {
            getCommand("rtpq").setExecutor(rtpqCmd);
            getCommand("rtpq").setTabCompleter(rtpqCmd);
        }
        // Legacy /mchashqueue command if defined
        String hashCmd = configManager.getQueueHashCommand();
        if (getCommand(hashCmd) != null && !hashCmd.equalsIgnoreCase("rtpq") && !hashCmd.equalsIgnoreCase("rtpqueue")) {
            getCommand(hashCmd).setExecutor(rtpqCmd);
            getCommand(hashCmd).setTabCompleter(rtpqCmd);
        }

        RynixRtpqCommand rtpqueueCmd = new RynixRtpqCommand(this);
        if (getCommand("rtpqueue") != null) {
            getCommand("rtpqueue").setExecutor(rtpqueueCmd);
            getCommand("rtpqueue").setTabCompleter(rtpqueueCmd);
        }

        // Also register /rtp if not already
        try {
            if (getCommand("rtp") != null && getCommand("rtp").getExecutor() == null) {
                getCommand("rtp").setExecutor(rtpqCmd);
                getCommand("rtp").setTabCompleter(rtpqCmd);
            }
        } catch (Exception ignored) {}

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this, gui, worldMenuGUI, inviteMenuGUI), this);

        queueManager.startQueueTask();

        LoggerUtil.info("§aRynixRtpq v2.0 Duo enabled! Commands: /rtpq (GUI), /rtpqueue (GUI) | Duo: " + configManager.getDuoSize() + " players 1 spot | Worlds: " + configManager.getWorldConfigs().size());
        LoggerUtil.info("§dMcPlugin format loaded: menu size " + configManager.getMenuSize() + " | world-menu " + configManager.getWorldMenuSize() + " | invite-menu " + configManager.getInviteMenuSize());
        LoggerUtil.logAuthor();
    }

    @Override
    public void onDisable() {
        if (queueManager != null) queueManager.clearQueue();
        if (scheduler != null) scheduler.cancelAll(this);
        LoggerUtil.logShutdown();
    }

    public static RynixRtpqPlugin getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public QueueManager getQueueManager() { return queueManager; }
    public RTPService getRTPService() { return rtpService; }
    public CooldownManager getCooldownManager() { return cooldownManager; }
    public RtpqGUI getGui() { return gui; }
    public WorldMenuGUI getWorldMenuGUI() { return worldMenuGUI; }
    public InviteMenuGUI getInviteMenuGUI() { return inviteMenuGUI; }
    public SchedulerAdapter getScheduler() { return scheduler; }
    public InviteManager getInviteManager() { return inviteManager; }
    public SoundManager getSoundManager() { return soundManager; }
    public boolean isFolia() { return isFolia; }
}
