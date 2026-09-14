package kireiko.dev.anticheat.listeners;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import java.util.Collections;
import kireiko.dev.anticheat.api.data.PlayerContainer;
import kireiko.dev.anticheat.api.player.PlayerProfile;
import kireiko.dev.anticheat.utils.version.VersionUtil;
import org.jetbrains.annotations.NotNull;

public final class VehicleTeleportListener extends PacketListener {

    public VehicleTeleportListener() {
        super(
                PacketListenerPriority.HIGHEST,
                Collections.singletonList(VersionUtil.getVersion().isNewerThanOrEquals(ServerVersion.V_1_21_2)
                                          ? PacketType.Play.Client.VEHICLE_MOVE
                                          : PacketType.Play.Client.STEER_VEHICLE)
        );
    }

    @Override
    public void onPacketReceiving(@NotNull PacketReceiveEvent event) {
        PlayerProfile protocol = PlayerContainer.getProfile(event.getPlayer());
        if (protocol == null) {
            return;
        }
        protocol.setLastTeleport(System.currentTimeMillis());
        protocol.setIgnoreFirstTick(true);
    }

}