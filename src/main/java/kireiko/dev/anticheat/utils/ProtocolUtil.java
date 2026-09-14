package kireiko.dev.anticheat.utils;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.protocol.PacketSide;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class ProtocolUtil {

    private static @NotNull PacketWrapper<?> createWrapper(@NotNull ProtocolPacketEvent event, @NotNull Class<? extends PacketWrapper<?>> wrapperClazz, Class<? extends ProtocolPacketEvent> eventClazz) {
        try {
            var wrapperConstructor = Objects.requireNonNull(wrapperClazz).getDeclaredConstructor(eventClazz);
            return wrapperConstructor.newInstance(event);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static @NotNull PacketWrapper<?> getOrCreateWrapper(@NotNull ProtocolPacketEvent event) {
        if (event.getLastUsedWrapper() != null) {
            return event.getLastUsedWrapper();
        }
        return createWrapper(event, Objects.requireNonNull(event.getPacketType().getWrapperClass()), event.getPacketType().getSide() == PacketSide.SERVER ? PacketSendEvent.class : PacketReceiveEvent.class);
    }

    @SuppressWarnings("unchecked")
    public static <T extends PacketWrapper<T>> @NotNull T getOrCreateWrapper(@NotNull Class<T> clazz, @NotNull PacketSide side, @NotNull ProtocolPacketEvent event) {
        if (clazz.isInstance(event.getLastUsedWrapper())) {
            return (T) event.getLastUsedWrapper();
        }
        return (T) createWrapper(event, clazz, side == PacketSide.SERVER ? PacketSendEvent.class : PacketReceiveEvent.class);
    }

    public static World readWorld(ProtocolPacketEvent event) {
        return ((Player) event.getPlayer()).getWorld();
    }

    public static boolean hasPosition(PacketTypeCommon type) {
        return type == PacketType.Play.Client.PLAYER_POSITION
                       || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION;
    }

    public static boolean hasRotation(PacketTypeCommon type) {
        return type == PacketType.Play.Client.PLAYER_ROTATION
                       || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION;
    }
}
