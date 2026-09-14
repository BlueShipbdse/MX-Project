package kireiko.dev.anticheat.listeners;


import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.PacketSide;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import java.util.Collections;
import kireiko.dev.anticheat.MX;
import kireiko.dev.anticheat.api.data.PlayerContainer;
import kireiko.dev.anticheat.api.events.AttackEntityEvent;
import kireiko.dev.anticheat.api.player.PlayerProfile;
import kireiko.dev.anticheat.utils.ConfigCache;
import kireiko.dev.anticheat.utils.ProtocolUtil;
import kireiko.dev.anticheat.utils.cache.EntityCache;
import kireiko.dev.anticheat.utils.version.VersionUtil;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class EntityAttackListener extends PacketListener {

    public EntityAttackListener() {
        super(PacketListenerPriority.HIGHEST,
              Collections.singletonList(VersionUtil.getVersion().isNewerThanOrEquals(ServerVersion.V_26_1)
                                                                        ? PacketType.Play.Client.ATTACK
                                                                        : PacketType.Play.Client.INTERACT_ENTITY)
         );
    }

    @SneakyThrows
    @Override
    public void onPacketReceiving(@NotNull PacketReceiveEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = PlayerContainer.getProfile(player);
        if (profile == null) {
            return;
        }
        boolean attack;
        int entityId;
        if (VersionUtil.getVersion().isNewerThanOrEquals(ServerVersion.V_26_1)) {
            var packet = ProtocolUtil.getOrCreateWrapper(WrapperPlayClientAttack.class, PacketSide.CLIENT, event);
            attack = true;
            entityId = packet.getEntityId();
        } else {
            var packet = ProtocolUtil.getOrCreateWrapper(WrapperPlayClientInteractEntity.class, PacketSide.CLIENT, event);
            attack = packet.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK;
            entityId = packet.getEntityId();
        }
        Entity entity = EntityCache.get(entityId);
        if (profile.getAttackBlockToTime() > System.currentTimeMillis()) {
            if (ConfigCache.PREVENTION > 0) {
                event.setCancelled(true);
                if (ConfigCache.PREVENTION >= 3) {
                    Bukkit.getScheduler().runTask(MX.getInstance(), () -> {
                        player.teleport(player.getLocation());
                    });
                } else if (ConfigCache.PREVENTION == 1
                                && attack
                                && entity instanceof LivingEntity target
                                && player.getLocation().toVector().distance(entity.getLocation().toVector()) < 3.3) {
                    Bukkit.getScheduler().runTask(MX.getInstance(), () -> {
                        target.damage(0.5, player);
                    });
                }
                profile.debug("Entity interact packet blocked");
                MX.blockedPerMinuteCount++;
            }
        }
        AttackEntityEvent e = new AttackEntityEvent(entity, attack, entityId, false);
        profile.run(e);
        if (e.isCancelled()) {
            event.setCancelled(true);
            profile.debug("Entity interact packet blocked after checking");
            MX.blockedPerMinuteCount++;
        }
    }
}
