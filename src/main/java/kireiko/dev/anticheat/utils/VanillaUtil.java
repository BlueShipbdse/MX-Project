package kireiko.dev.anticheat.utils;

import net.minecraft.server.level.ServerLevel;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class VanillaUtil {
    public static @NotNull ServerLevel toNMSWorld(@NotNull World world) {
        return ((CraftWorld) world).getHandle();
    }
    public static @Nullable Entity getEntityFromId(@NotNull World world, int entId) {
        var nmsEnt = toNMSWorld(world).getEntity(entId);
        if (nmsEnt == null) {
            return null;
        }
        return nmsEnt.getBukkitEntity();
    }
}
