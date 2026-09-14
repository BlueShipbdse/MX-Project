package kireiko.dev.anticheat.listeners;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public abstract class PacketListener extends PacketListenerAbstract {
    private final @NotNull Collection<PacketTypeCommon> packetTypes;

    public PacketListener(@NotNull PacketListenerPriority priority, @NotNull Collection<PacketTypeCommon> packetTypes) {
        super(priority);
        this.packetTypes = packetTypes;
    }

    /**
     * Called when a packet is sent that matches the packet types
     * given on initialization.
     * @param event the packet receive event
     */
    public void onPacketReceiving(@NotNull PacketReceiveEvent event) {}

    public void onPacketSending(@NotNull PacketSendEvent event) {}


    @Override
    public final void onPacketReceive(PacketReceiveEvent event) {
        if (packetTypes.contains(event.getPacketType())) {
            onPacketReceiving(event);
        }
    }

    @Override
    public final void onPacketSend(PacketSendEvent event) {
        if (packetTypes.contains(event.getPacketType())) {
            onPacketSending(event);
        }
    }
}
