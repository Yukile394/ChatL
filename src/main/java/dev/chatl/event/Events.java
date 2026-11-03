package dev.chatl.event;

import dev.chatl.model.KnockbackRecord;
import dev.chatl.model.TargetInfo;
import dev.chatl.module.ChatModule;

public final class Events {
    private Events() { }
    public record ModuleToggled(ChatModule module, boolean enabled) { }
    public record TargetChanged(TargetInfo target) { }
    public record FlightStateChanged(boolean flying, boolean elytraEquipped) { }
    public record KnockbackRecorded(KnockbackRecord record) { }
}
