package kireiko.dev.anticheat.api.events;

import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
@AllArgsConstructor
public final class EntityActionEvent {
    private @NotNull WrapperPlayClientEntityAction.Action action;
}
