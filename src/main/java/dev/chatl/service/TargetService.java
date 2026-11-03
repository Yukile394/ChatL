package dev.chatl.service;

import dev.chatl.config.ConfigManager;
import dev.chatl.event.EventBus;
import dev.chatl.event.Events;
import dev.chatl.model.TargetInfo;
import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** AimAssist ve TriggerBot'un ortak hedef veri modeli. Yalnizca OKUR; kamera/saldiri uzerinde etkisi yoktur. */
public final class TargetService {
    private static volatile TargetInfo current;

    private TargetService() { }

    public static TargetInfo get() { return current; }

    public static void clear() { current = null; }

    public static void update(MinecraftClient mc, double range) {
        if (mc.player == null || mc.world == null) { current = null; return; }
        AbstractClientPlayerEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || !p.isAlive() || p.isSpectator()) continue;
            if (ConfigManager.friends().contains(p.getName().getString())) continue;
            double d = mc.player.distanceTo(p);
            if (d <= range && d < bestDist) { best = p; bestDist = d; }
        }
        TargetInfo prev = current;
        if (best == null) {
            current = null;
        } else {
            Vec3d to = best.getBoundingBox().getCenter().subtract(mc.player.getEyePos());
            float yawTo = (float) Math.toDegrees(Math.atan2(-to.x, to.z));
            float pitchTo = (float) -Math.toDegrees(Math.atan2(to.y, Math.hypot(to.x, to.z)));
            float dyaw = MathHelper.wrapDegrees(yawTo - mc.player.getYaw());
            float dpitch = MathHelper.wrapDegrees(pitchTo - mc.player.getPitch());
            Entity ch = mc.crosshairTarget instanceof EntityHitResult ehr ? ehr.getEntity() : null;
            current = new TargetInfo(best.getName().getString(), bestDist, dyaw, dpitch, ch == best);
        }
        String a = prev == null ? null : prev.name();
        String b = current == null ? null : current.name();
        if (!Objects.equals(a, b)) EventBus.post(new Events.TargetChanged(current));
    }
}
