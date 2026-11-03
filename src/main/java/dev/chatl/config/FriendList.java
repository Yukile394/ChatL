package dev.chatl.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FriendList {
    private final Path file;
    private final List<String> names = new ArrayList<>();

    public FriendList(Path file) { this.file = file; }

    public List<String> list() { return Collections.unmodifiableList(names); }

    public boolean contains(String n) {
        if (n == null) return false;
        for (String s : names) if (s.equalsIgnoreCase(n)) return true;
        return false;
    }

    /** Basariliysa null, degilse kullaniciya gosterilecek hata mesaji. */
    public String add(String raw) {
        String n = raw == null ? "" : raw.trim();
        if (n.isEmpty()) return "Kullanici adi bos olamaz.";
        if (!n.matches("[A-Za-z0-9_]{3,16}")) return "Gecersiz ad (3-16 karakter, harf/rakam/_).";
        if (contains(n)) return "Zaten listede.";
        if (names.size() >= 200) return "Liste dolu (en fazla 200).";
        names.add(n);
        names.sort(String.CASE_INSENSITIVE_ORDER);
        save();
        return null;
    }

    public boolean remove(String n) {
        boolean r = names.removeIf(s -> s.equalsIgnoreCase(n));
        if (r) save();
        return r;
    }

    public void load() {
        names.clear();
        JsonObject o = JsonStore.read(file);
        if (o == null || !o.has("friends") || !o.get("friends").isJsonArray()) return;
        for (JsonElement e : o.getAsJsonArray("friends")) {
            if (!e.isJsonPrimitive()) continue;
            String n = e.getAsString();
            if (n.matches("[A-Za-z0-9_]{3,16}") && !contains(n)) names.add(n);
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
    }

    public void save() {
        JsonObject o = new JsonObject();
        JsonArray a = new JsonArray();
        names.forEach(a::add);
        o.add("friends", a);
        JsonStore.write(file, o);
    }
}
