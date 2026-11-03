package dev.chatl;

import dev.chatl.config.ConfigManager;
import dev.chatl.gui.ClickGuiScreen;
import dev.chatl.gui.CompatScreen;
import dev.chatl.hud.HudRenderer;
import dev.chatl.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class ChatLClient implements ClientModInitializer {
    public static KeyBinding OPEN_GUI;
    public static KeyBinding OPEN_COMPAT;
    public static long ticks;

    @Override
    public void onInitializeClient() {
        ConfigManager.init(FabricLoader.getInstance().getConfigDir().resolve("chatl"));
        ConfigManager.load();

        OPEN_GUI = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.chatl.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, "key.categories.chatl"));
        OPEN_COMPAT = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.chatl.compat", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_P, "key.categories.chatl"));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> ConfigManager.save());
        HudRenderer.register();
    }

    private void onTick(MinecraftClient mc) {
        ticks++;
        while (OPEN_GUI.wasPressed()) {
            if (mc.currentScreen == null) mc.setScreen(new ClickGuiScreen());
        }
        while (OPEN_COMPAT.wasPressed()) {
            if (mc.currentScreen == null) mc.setScreen(new CompatScreen());
        }
        if (mc.player == null || mc.world == null) return;
        ModuleManager.tick(mc, mc.currentScreen == null);
    }

    public static void notice(String msg) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.inGameHud != null) mc.inGameHud.setOverlayMessage(Text.literal("[ChatL] " + msg), false);
    }
}
