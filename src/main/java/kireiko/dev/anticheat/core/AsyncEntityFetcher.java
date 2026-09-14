package kireiko.dev.anticheat.core;

import java.util.concurrent.CompletableFuture;
import kireiko.dev.anticheat.MX;
import kireiko.dev.anticheat.utils.VanillaUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;

public final class AsyncEntityFetcher {
    public static CompletableFuture<Entity> getEntityFromIDAsync(final World world, final int entityId) {
        CompletableFuture<Entity> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(MX.getInstance(), () -> {
            try {
                Entity entity = VanillaUtil.getEntityFromId(world, entityId);
                future.complete(entity);
            } catch (Exception ex) {
                future.completeExceptionally(ex);
            }
        });

        return future;
    }
}
