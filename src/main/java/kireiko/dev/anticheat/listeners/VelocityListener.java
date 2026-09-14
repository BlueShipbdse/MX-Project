package kireiko.dev.anticheat.listeners;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;
import java.util.Collections;
import kireiko.dev.anticheat.api.data.PlayerContainer;
import kireiko.dev.anticheat.api.events.SVelocityEvent;
import kireiko.dev.anticheat.api.player.PlayerProfile;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public final class VelocityListener extends PacketListener {


    public VelocityListener() {
        super(
                PacketListenerPriority.MONITOR,
                Collections.singletonList(PacketType.Play.Server.ENTITY_VELOCITY)
        );
    }

    @Override
    public void onPacketSending(@NotNull PacketSendEvent event) {
        final Player player = event.getPlayer();
        final PlayerProfile protocol = PlayerContainer.getProfile(player);
        if (protocol == null) {
            return;
        }
        var packet = new WrapperPlayServerEntityVelocity(event);
        int entID = packet.getEntityId();
        if (protocol.getEntityId() == entID) {
            var velocity = packet.getVelocity();
            double x = velocity.x / 8000.0D,
                            y = velocity.y / 8000.0D,
                            z = velocity.z / 8000.0D;
            SVelocityEvent velocityEvent = new SVelocityEvent(new Vector(x, y, z));
            protocol.run(velocityEvent);
        }
    }
}
