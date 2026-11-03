package dev.chatl;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import dev.chatl.config.FriendList;
import dev.chatl.config.JsonStore;
import dev.chatl.event.EventBus;
import dev.chatl.module.ChatModule;
import dev.chatl.service.Search;
import dev.chatl.setting.Setting;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CoreTest {
    static class Dummy extends ChatModule {
        final Setting.Num range = add(new Setting.Num("Menzil", 3, 12, 0.5, 6));
        final Setting.Bool hud = add(new Setting.Bool("HUD'da goster", true));
        Dummy(String n) { super(n, "d", Category.COMBAT); }
    }

    @Test void searchOnlyMatchesNameAndOwnSettings() {
        Dummy v = new Dummy("Velocity");
        Dummy a = new Dummy("AimAssist");
        assertTrue(Search.matches(v, "velocity"));
        assertFalse(Search.matches(a, "velocity"));
        assertTrue(Search.matches(a, "MENZIL"));
        assertEquals("Menzil", Search.matchedSetting(a, "menz"));
        assertFalse(Search.matches(a, "scaffold"));
    }

    @Test void moduleConfigRoundTrip() {
        Dummy m = new Dummy("X");
        m.range.set(9.5); m.hud.set(false); m.setKey(80); m.setEnabled(true);
        JsonObject o = m.toJson();
        Dummy n = new Dummy("X");
        n.fromJson(o);
        assertEquals(9.5, n.range.get());
        assertFalse(n.hud.get());
        assertEquals(80, n.key());
        assertTrue(n.isEnabled());
    }

    @Test void numClampsAndSteps() {
        Setting.Num n = new Setting.Num("a", 0, 10, 0.5, 5);
        n.set(99); assertEquals(10, n.get());
        n.set(2.74); assertEquals(2.5, n.get());
        n.set(Double.NaN); assertEquals(2.5, n.get());
    }

    @Test void corruptFileFallsBackToDefaults(@TempDir Path dir) throws Exception {
        Path p = dir.resolve("modules.json");
        Files.writeString(p, "{ bozuk json ");
        assertNull(JsonStore.read(p));
        assertTrue(Files.exists(dir.resolve("modules.json.bad")));
    }

    @Test void friendListRules(@TempDir Path dir) {
        FriendList f = new FriendList(dir.resolve("friends.json"));
        assertNull(f.add("Zed_1"));
        assertNull(f.add("alpha"));
        assertNotNull(f.add("alpha"));
        assertNotNull(f.add("x"));
        assertNotNull(f.add("bad name"));
        assertEquals("alpha", f.list().get(0));
        FriendList g = new FriendList(dir.resolve("friends.json"));
        g.load();
        assertEquals(2, g.list().size());
        assertTrue(g.contains("ZED_1"));
        assertTrue(g.remove("Alpha"));
        assertEquals(1, g.list().size());
    }

    @Test void eventBusUnsubscribe() {
        record Ping() { }
        AtomicInteger c = new AtomicInteger();
        Runnable off = EventBus.subscribe(Ping.class, e -> c.incrementAndGet());
        EventBus.post(new Ping());
        off.run();
        EventBus.post(new Ping());
        assertEquals(1, c.get());
        assertEquals(0, EventBus.listenerCount(Ping.class));
    }

    @Test void moduleToggleDoesNotAffectOthers() {
        Dummy a = new Dummy("A"), b = new Dummy("B");
        a.setEnabled(true); b.setEnabled(true); a.setEnabled(false);
        assertTrue(b.isEnabled());
    }
}
