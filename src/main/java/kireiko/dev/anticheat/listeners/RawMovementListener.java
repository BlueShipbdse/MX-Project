package kireiko.dev.anticheat.listeners;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.PacketSide;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import java.util.Arrays;
import kireiko.dev.anticheat.api.data.PlayerContainer;
import kireiko.dev.anticheat.api.data.RotationsContainer;
import kireiko.dev.anticheat.api.events.MoveEvent;
import kireiko.dev.anticheat.api.events.NoRotationEvent;
import kireiko.dev.anticheat.api.events.RotationEvent;
import kireiko.dev.anticheat.api.player.PlayerProfile;
import kireiko.dev.anticheat.api.player.SensitivityProcessor;
import kireiko.dev.anticheat.utils.ConfigCache;
import kireiko.dev.anticheat.utils.ProtocolUtil;
import kireiko.dev.millennium.vectors.Vec2f;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class RawMovementListener extends PacketListener {
    public RawMovementListener() {
        super(
                PacketListenerPriority.LOWEST,
                Arrays.asList(
                        PacketType.Play.Server.PLAYER_POSITION_AND_LOOK,
                        PacketType.Play.Client.PLAYER_POSITION,
                        PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION,
                        PacketType.Play.Client.PLAYER_ROTATION,
                        PacketType.Play.Client.PLAYER_FLYING
                )
        );
    }

    @Override
    public void onPacketSending(@NotNull PacketSendEvent event) {
        final Player player = event.getPlayer();
        final PlayerProfile profile = PlayerContainer.getProfile(player);
        if (profile == null) {
            return;
        }
        profile.setLastTeleport(System.currentTimeMillis());
        profile.setIgnoreFirstTick(true);
    }

    @Override
    public void onPacketReceiving(@NotNull PacketReceiveEvent event) {
        final Player player = event.getPlayer();
        final PlayerProfile profile = PlayerContainer.getProfile(player);
        if (profile == null) {
            return;
        }
        WrapperPlayClientPlayerFlying packet = ProtocolUtil.getOrCreateWrapper(WrapperPlayClientPlayerFlying.class, PacketSide.CLIENT, event);
        profile.setGround(packet.isOnGround());
        profile.setAirTicks((profile.isGround()) ? 0 : profile.getAirTicks() + 1);
        profile.setFrom(profile.getTo().clone());
        var packetLoc = packet.getLocation();
        boolean hasPosition = packet.hasPositionChanged();
        boolean hasRotation = packet.hasRotationChanged();

        Location l = profile.getTo().clone();

        l.setWorld(ProtocolUtil.readWorld(event));

        if (hasPosition) {
            double[] v = new double[]{packetLoc.getX(), packetLoc.getY(), packetLoc.getZ()};
            for (Double check : v)
                if (check.isNaN() || check.isInfinite() || Math.abs(check) > 3E8) {
                    return;
                }
            l.setX(packetLoc.getX());
            l.setY(packetLoc.getY());
            l.setZ(packetLoc.getZ());
        }

        if (hasRotation) {
            Float yaw = packetLoc.getYaw();
            if (yaw.isNaN() || yaw.isInfinite() || Math.abs(yaw) > 3E8) {
                return;
            }
            Float pitch = packetLoc.getPitch();
            if (pitch.isNaN() || pitch.isInfinite() || Math.abs(pitch) > 3E8) {
                return;
            }
            l.setYaw(yaw);
            l.setPitch(pitch);
        }

        profile.setTo(l.clone());


        if (hasRotation) {
            SensitivityProcessor controller = profile.getSensitivityProcessor();
            controller.setLastDeltaPitch(controller.getLastDeltaPitch());
            Vec2f from = new Vec2f(profile.getFrom().getYaw(), profile.getFrom().getPitch());
            Vec2f to = new Vec2f(profile.getTo().getYaw(), profile.getTo().getPitch());
            RotationEvent rotationEvent = new RotationEvent(profile, to, from);
            controller.setDeltaPitch(rotationEvent.getDelta().getY());
            controller.processSensitivity();
            boolean isTeleporting = (System.currentTimeMillis() - profile.getLastTeleport() < 500) || profile.isIgnoreFirstTick();

            if (ConfigCache.ROTATIONS_CONTAINER
                            && !profile.isIgnoreFirstTick()
                            && !isTeleporting) {
                RotationsContainer.register(player.getUniqueId(), rotationEvent.getDelta());
            }

            profile.getCinematicComponent().process(rotationEvent);
            if (!isTeleporting) {
                profile.run(rotationEvent);
            }
            profile.setIgnoreFirstTick(false);
        } else {
            if (!profile.isIgnoreFirstTick() && profile.getLastTeleport() + 1000 < System.currentTimeMillis()) {
                if (profile.getTo().toVector().distance(profile.getFrom().toVector()) > 1e-4) {
                    profile.run(new NoRotationEvent(profile));
                }
            }
        }

        profile.getPastLoc().add(profile.getTo());
        profile.run(new MoveEvent(profile, profile.getTo(), profile.getFrom()));

        if (profile.transactionBoot) LatencyHandler.startChecking(profile);
    }
}
