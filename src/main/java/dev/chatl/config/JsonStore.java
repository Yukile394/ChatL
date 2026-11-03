package dev.chatl.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import dev.chatl.util.ChatLog;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class JsonStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private JsonStore() { }

    /** Dosya yoksa null. Bozuksa .bad olarak yedeklenir ve null doner (varsayilanlara guvenli donus). */
    public static JsonObject read(Path p) {
        if (p == null || !Files.exists(p)) return null;
        try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            JsonElement e = JsonParser.parseReader(r);
            if (e != null && e.isJsonObject()) return e.getAsJsonObject();
            throw new JsonParseException("kok nesne degil");
        } catch (IOException | RuntimeException ex) {
            ChatLog.warn("Bozuk yapilandirma: " + p.getFileName() + " (" + ex.getMessage() + ") - yedeklendi, varsayilanlar kullaniliyor");
            try { Files.move(p, p.resolveSibling(p.getFileName() + ".bad"), StandardCopyOption.REPLACE_EXISTING); } catch (IOException ignored) { }
            return null;
        }
    }

    public static void write(Path p, JsonObject o) {
        if (p == null) return;
        try {
            Files.createDirectories(p.getParent());
            Path tmp = p.resolveSibling(p.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(o), StandardCharsets.UTF_8);
            Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            ChatLog.error("Yapilandirma yazilamadi: " + p.getFileName() + " (" + ex.getMessage() + ")");
        }
    }
}
