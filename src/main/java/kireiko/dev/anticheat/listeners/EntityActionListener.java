package kireiko.dev.anticheat.listeners;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import java.util.Collections;
import kireiko.dev.anticheat.api.data.PlayerContainer;
import kireiko.dev.anticheat.api.events.EntityActionEvent;
import kireiko.dev.anticheat.api.player.PlayerProfile;
import org.jetbrains.annotations.NotNull;

public final class EntityActionListener extends PacketListener {

    public EntityActionListener() {
        super(
                PacketListenerPriority.HIGHEST,
                Collections.singletonList(PacketType.Play.Client.ENTITY_ACTION)
        );
    }

    @Override
    public void onPacketReceiving(@NotNull PacketReceiveEvent event) {
        PlayerProfile protocol = PlayerContainer.getProfile(event.getPlayer());
        if (protocol == null) {
            return;
        }

        var packet = new WrapperPlayClientEntityAction(event);
        var action = packet.getAction();

        switch (action) {
            case START_SNEAKING -> {
                protocol.sneaking = true;
            }
            case STOP_SNEAKING -> {
                protocol.sneaking = false;
            }
            case START_SPRINTING -> {
                protocol.sprinting = true;
            }
            case STOP_SPRINTING -> {
                protocol.sprinting = false;
            }
        }
        EntityActionEvent e = new EntityActionEvent(action);
        protocol.run(e);
    }
}