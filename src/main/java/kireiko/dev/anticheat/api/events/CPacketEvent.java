package kireiko.dev.anticheat.api.events;


import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class CPacketEvent {
    private ProtocolPacketEvent packetEvent;
}
