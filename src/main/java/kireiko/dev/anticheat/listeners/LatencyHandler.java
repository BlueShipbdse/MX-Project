package kireiko.dev.anticheat.listeners;


import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.PacketSide;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientKeepAlive;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPong;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerKeepAlive;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPing;
import java.util.Arrays;
import kireiko.dev.anticheat.MX;
import kireiko.dev.anticheat.api.data.PlayerContainer;
import kireiko.dev.anticheat.api.events.CTransactionEvent;
import kireiko.dev.anticheat.api.player.PlayerProfile;
import kireiko.dev.anticheat.utils.ProtocolUtil;
import kireiko.dev.anticheat.utils.version.VersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class LatencyHandler extends PacketListener {

    public LatencyHandler() {
        super(
                PacketListenerPriority.MONITOR,
                Arrays.asList(VersionUtil.getVersion().isNewerThanOrEquals(ServerVersion.V_1_17) ? PacketType.Play.Client.PONG
                                                                                                 : PacketType.Play.Client.KEEP_ALIVE,
                              VersionUtil.getVersion().isNewerThanOrEquals(ServerVersion.V_1_17) ? PacketType.Play.Server.PING
                                                                                                 : PacketType.Play.Server.KEEP_ALIVE)
        );
    }

    public static void startChecking(PlayerProfile protocol) {
        protocol.transactionId = -1963;
        protocol.transactionBoot = false;
        sendTransaction(protocol, protocol.transactionId);
    }

    public static void sendTransaction(PlayerProfile protocol, long id) {
        var packet = VersionUtil.getVersion().isNewerThanOrEquals(ServerVersion.V_1_17) ? new WrapperPlayServerPing((int) id)
                                                                                        : new WrapperPlayServerKeepAlive(id);

        PacketEvents.getAPI().getPlayerManager().sendPacket(protocol.getPlayer(), packet);
        protocol.transactionId--;
        if (protocol.transactionId < -1987)
            protocol.transactionId = -1963;
    }

    @Override
    public void onPacketReceiving(@NotNull PacketReceiveEvent event) {
        final Player player = event.getPlayer();
        final PlayerProfile protocol = PlayerContainer.getProfile(player);
        if (protocol == null) {
            return;
        }
        long id;

        if (VersionUtil.getVersion().isNewerThanOrEquals(ServerVersion.V_1_17)) {
            var packet = ProtocolUtil.getOrCreateWrapper(WrapperPlayClientPong.class, PacketSide.CLIENT, event);
            id = packet.getId();
        } else {
            var packet = ProtocolUtil.getOrCreateWrapper(WrapperPlayClientKeepAlive.class, PacketSide.CLIENT, event);
            id = packet.getId();
        }
        if (id <= -1963 && id >= -1987) {
            protocol.transactionPing = System.currentTimeMillis() - protocol.transactionTime;
            protocol.getPing().add(protocol.transactionPing);
            protocol.transactionLastTime = System.currentTimeMillis();
            protocol.transactionSentKeep = false;
            CTransactionEvent transactionEvent = new CTransactionEvent(protocol);
            protocol.run(transactionEvent);
            Bukkit.getScheduler().runTaskLaterAsynchronously(MX.getInstance(),
                    () -> sendTransaction(protocol, protocol.transactionId), 200L);
        }
    }

    @Override
    public void onPacketSending(@NotNull PacketSendEvent event) {
        final Player player = event.getPlayer();
        final PlayerProfile protocol = PlayerContainer.getProfile(player);
        if (protocol == null) {
            return;
        }
        protocol.transactionSentKeep = true;
        protocol.transactionTime = System.currentTimeMillis();
    }
}