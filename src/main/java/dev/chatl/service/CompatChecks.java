package dev.chatl.service;

import dev.chatl.event.Events;
import dev.chatl.event.EventBus;
import dev.chatl.module.ModuleManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;

/** P panelindeki canli uyumluluk testleri. Hicbir sey degistirmez, yalnizca okur. */
public final class CompatChecks {
    public record Result(String name, boolean ok, String detail) { }

    private CompatChecks() { }

    public static List<Result> runAll() {
        List<Result> r = new ArrayList<>();
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        boolean inWorld = p != null && mc.world != null;
        r.add(new Result("Oyuncu ve dunya yuklu", inWorld, inWorld ? "tamam" : "bir dunyaya gir"));
        if (!inWorld) return r;

        r.add(safe("ElytraFix: govde slotu okunuyor", () -> {
            boolean e = p.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
            return new Result("ElytraFix: govde slotu okunuyor", true, e ? "Elytra takili" : "Elytra takili degil");
        }));
        r.add(safe("ElytraFix: durum tutarliligi", () -> {
            boolean bad = p.isFallFlying() && !p.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
            return new Result("ElytraFix: durum tutarliligi", !bad, bad ? "ucus aktif ama Elytra yok" : "tutarli");
        }));
        r.add(safe("FireworkFix: hotbar taramasi", () -> {
            int n = 0;
            for (int i = 0; i < 9; i++) if (p.getInventory().getStack(i).isOf(Items.FIREWORK_ROCKET)) n++;
            return new Result("FireworkFix: hotbar taramasi", true, n + " slotta fisek");
        }));
        r.add(safe("Elytra <-> Firework olay baglantisi", () -> {
            boolean both = ModuleManager.ELYTRA.isEnabled() && ModuleManager.FIREWORK.isEnabled();
            if (!both) return new Result("Elytra <-> Firework olay baglantisi", true, "atlandi (iki modul de acik degil)");
            Boolean sf = ModuleManager.FIREWORK.sharedFlight();
            boolean ok = sf == null || sf == ModuleManager.ELYTRA.isFlying();
            return new Result("Elytra <-> Firework olay baglantisi", ok, sf == null ? "henuz gecis olayi yok" : ok ? "eslesiyor" : "durumlar farkli");
        }));
        r.add(safe("Olay dinleyicileri", () -> {
            int n = EventBus.listenerCount(Events.FlightStateChanged.class);
            boolean ok = !ModuleManager.FIREWORK.isEnabled() || n > 0;
            return new Result("Olay dinleyicileri", ok, n + " FlightStateChanged dinleyicisi");
        }));
        return r;
    }

    private interface Check { Result run(); }

    private static Result safe(String name, Check c) {
        try { return c.run(); } catch (RuntimeException ex) { return new Result(name, false, "hata: " + ex.getClass().getSimpleName()); }
    }
}
