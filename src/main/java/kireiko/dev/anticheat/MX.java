package kireiko.dev.anticheat;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import kireiko.dev.anticheat.api.data.Metrics;
import kireiko.dev.anticheat.api.data.PlayerContainer;
import kireiko.dev.anticheat.api.player.PlayerProfile;
import kireiko.dev.anticheat.commands.MXCommandHandler;
import kireiko.dev.anticheat.core.AsyncScheduler;
import kireiko.dev.anticheat.listeners.*;
import kireiko.dev.anticheat.managers.CheckManager;
import kireiko.dev.anticheat.services.SimulationFlagService;
import kireiko.dev.anticheat.utils.ConfigCache;
import kireiko.dev.anticheat.utils.version.VersionUtil;
import kireiko.dev.millennium.ml.ClientML;
import kireiko.dev.millennium.types.EvictingList;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class MX extends JavaPlugin {

    public static final String
            command = "mx",
            name = "MX",
            permissionHead = "mx.",
            permission = permissionHead + "admin";
    public static int bannedPerMinuteCount = 0;
    public static List<Integer> bannedPerMinuteList = new EvictingList<>(60);
    public static int blockedPerMinuteCount = 0;
    public static List<Integer> blockedPerMinuteList = new EvictingList<>(60);
    @Getter
    private static MX instance;

    @Override
    public void onEnable() {
        instance = this;
        CheckManager.init();
        saveDefaultConfig();
        ConfigCache.loadConfig();
        kireiko.dev.anticheat.managers.DatasetManager.init();

        getLogger().info("Loading listeners...");
        loadListeners();
        getLogger().info("Booting timers...");
        punishTimer();
        getLogger().info("Initializing commands...");
        PluginCommand pCommand = this.getCommand(command);
        if (pCommand != null) {
            MXCommandHandler handler = new MXCommandHandler();
            pCommand.setExecutor(handler);
            pCommand.setTabCompleter(handler);
        }
        getLogger().info("Running metrics...");
        final Metrics metrics = new Metrics(this, 25612);
        metrics.addCustomChart(new Metrics.SingleLineChart("banned_players_count", () -> {
            int banCount = 0;
            for (int i : MX.bannedPerMinuteList) banCount += i;
            return banCount;
        }));
        getLogger().info("Launching ML (Kireiko Millennium 5)...");
        ClientML.run();
        getLogger().info("Launched!\n"
                        + "        :::   :::       :::    :::\n" +
                        "      :+:+: :+:+:      :+:    :+:\n" +
                        "    +:+ +:+:+ +:+      +:+  +:+  \n" +
                        "   +#+  +:+  +#+       +#++:+\n" +
                        "  +#+       +#+      +#+  +#+\n" +
                        " #+#       #+#     #+#    #+#\n" +
                        "###       ###     ###    ###\n" +
                        "\nCreated by pawsashatoy (Kireiko Oleksandr)\n"
                        );
    }

    private void punishTimer() {
        SimulationFlagService.init();
        //CrasherShieldNewListener.watchdog();

        // reset vl
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            float r = ConfigCache.VL_RESET;
            bannedPerMinuteList.add(bannedPerMinuteCount);
            bannedPerMinuteCount = 0;
            blockedPerMinuteList.add(blockedPerMinuteCount);
            blockedPerMinuteCount = 0;
            for (PlayerProfile profile : PlayerContainer.getUuidPlayerProfileMap().values()) {
                profile.fade(r);
                profile.setFlagCount(0);
            }
        }, 20L, 1200L);
    }

    private void loadListeners() {
        Bukkit.getPluginManager().registerEvents(new JoinQuitListener(), this);
        var eventManager = PacketEvents.getAPI().getEventManager();
        EntityTrackerListener.register();
        eventManager.registerListener(new RawMovementListener());
        eventManager.registerListener(new EntityActionListener());
        eventManager.registerListener(new EntityAttackListener());
        eventManager.registerListener(new LatencyHandler());
        eventManager.registerListener(new VelocityListener());
        eventManager.registerListener(new VehicleTeleportListener());
        eventManager.registerListener(new InventoryListener());
        { // omni listener
            var version = VersionUtil.getVersion().toClientVersion();
            final Set<PacketTypeCommon> listeners = new HashSet<>();
            for (PacketTypeCommon packetType : PacketType.Play.Client.values()) {
                if (packetType.getId(version) > 0) {
                    listeners.add(packetType);
                }
            }

            eventManager.registerListener(new OmniPacketListener(listeners));
        }
    }

    @Override
    public void onDisable() {
        AsyncScheduler.shutdown();
    }

}
