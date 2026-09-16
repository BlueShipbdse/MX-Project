package kireiko.dev.anticheat.listeners;


import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.*;
import java.util.Arrays;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import static com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client.*;

public final class TestListener extends PacketListener {
    public TestListener() {
        super(PacketListenerPriority.HIGHEST,
              Arrays.asList(PacketType.Play.Client.values())
        );
    }

    @Override
    public void onPacketReceiving(@NonNull PacketReceiveEvent event) {
        switch (event.getPacketType()) {
            case PLAYER_POSITION, PONG, KEEP_ALIVE, PLAYER_POSITION_AND_ROTATION, PLAYER_ROTATION  -> {}
            case ANIMATION -> {
                WrapperPlayClientAnimation animation = new WrapperPlayClientAnimation(event);
                Player player = event.getPlayer();
                player.sendMessage("e: " + event.getPacketType() + ", " + animation.getHand());
            }
            case PLAYER_BLOCK_PLACEMENT -> {
                WrapperPlayClientPlayerBlockPlacement animation = new WrapperPlayClientPlayerBlockPlacement(event);
                Player player = event.getPlayer();
                player.sendMessage("e: " + event.getPacketType() + ", " + animation.getHand());
            }
            case USE_ITEM -> {
                WrapperPlayClientUseItem animation = new WrapperPlayClientUseItem(event);
                Player player = event.getPlayer();
                player.sendMessage("e: " + event.getPacketType() + ", " + animation.getHand());
            }
            case INTERACT_ENTITY -> {
                WrapperPlayClientInteractEntity animation = new WrapperPlayClientInteractEntity(event);
                Player player = event.getPlayer();
                player.sendMessage("e: " + event.getPacketType() + ", " + animation.getAction() + ", " + animation.getHand() + ", Sneaking? " + animation.isSneaking());
            }
            case PLAYER_DIGGING -> {
                WrapperPlayClientPlayerDigging animation = new WrapperPlayClientPlayerDigging(event);
                Player player = event.getPlayer();
                player.sendMessage("e: " + event.getPacketType() + ", " + animation.getAction());
            }
            default -> {
                Player player = event.getPlayer();
                player.sendMessage("e: " + event.getPacketType());
            }
        }
    }
}
