package kireiko.dev.anticheat.listeners;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import java.util.List;
import kireiko.dev.anticheat.api.data.PlayerContainer;
import kireiko.dev.anticheat.api.events.WindowClickEvent;
import kireiko.dev.anticheat.api.player.PlayerProfile;

import org.jetbrains.annotations.NotNull;

public final class InventoryListener extends PacketListener {

    public InventoryListener() {
        super(
                PacketListenerPriority.HIGHEST,
                List.of(PacketType.Play.Client.CLICK_WINDOW_BUTTON, PacketType.Play.Client.CLICK_WINDOW)
        );
    }

    @Override
    public void onPacketReceiving(@NotNull PacketReceiveEvent event) {
        PlayerProfile protocol = PlayerContainer.getProfile(event.getPlayer());
        if (protocol == null) {
            return;
        }
        protocol.run(new WindowClickEvent(event));
    }
}