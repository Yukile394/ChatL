package dev.chatl.config;

import dev.chatl.event.EventBus;
import dev.chatl.event.Events;
import dev.chatl.module.ModuleManager;
import java.nio.file.Path;

public final class ConfigManager {
    private static Path dir;
    private static FriendList friends = new FriendList(null);
    private static boolean loading;

    private ConfigManager() { }

    public static FriendList friends() { return friends; }

    public static void init(Path d) {
        dir = d;
        friends = new FriendList(d.resolve("friends.json"));
        EventBus.subscribe(Events.ModuleToggled.class, e -> save());
    }

    public static void load() {
        loading = true;
        try {
            ModuleManager.loadFrom(JsonStore.read(dir.resolve("modules.json")));
            GuiPrefs.load(JsonStore.read(dir.resolve("gui.json")));
            friends.load();
        } finally {
            loading = false;
        }
    }

    public static void save() {
        if (dir == null || loading) return;
        JsonStore.write(dir.resolve("modules.json"), ModuleManager.saveTo());
        JsonStore.write(dir.resolve("gui.json"), GuiPrefs.save());
        friends.save();
    }
}
