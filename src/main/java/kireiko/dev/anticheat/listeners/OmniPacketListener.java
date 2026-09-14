package kireiko.dev.anticheat.listeners;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import java.util.Set;
import kireiko.dev.anticheat.api.data.PlayerContainer;
import kireiko.dev.anticheat.api.events.CPacketEvent;
import kireiko.dev.anticheat.api.player.PlayerProfile;
import org.jspecify.annotations.NonNull;

public final class OmniPacketListener extends PacketListener {

    public OmniPacketListener(Set<PacketTypeCommon> list) {
        super(
                PacketListenerPriority.HIGHEST,
                list
        );
    }

    @Override
    public void onPacketReceiving(@NonNull PacketReceiveEvent event) {
        PlayerProfile protocol = PlayerContainer.getProfile(event.getPlayer());
        if (protocol == null) {
            return;
        }
        //protocol.getPlayer().sendMessage("i: " + event.getPacket().getType().name());
        protocol.run(new CPacketEvent(event));
    }
}