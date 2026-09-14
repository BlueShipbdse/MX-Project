package kireiko.dev.anticheat.listeners;





import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import java.util.Arrays;
import kireiko.dev.anticheat.utils.ProtocolUtil;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

public final class TestListener extends PacketListener {
    public TestListener() {
        super(PacketListenerPriority.HIGHEST,
              Arrays.asList(PacketType.Play.Client.values())
        );
    }

    @Override
    public void onPacketReceiving(@NonNull PacketReceiveEvent event) {
        Player player = event.getPlayer();
        player.sendMessage("e: " + event.getPacketType()
                + " " + ProtocolUtil.getOrCreateWrapper(event).readNBT());
    }
}
