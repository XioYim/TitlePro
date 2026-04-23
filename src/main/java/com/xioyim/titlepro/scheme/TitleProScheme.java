package com.xioyim.titlepro.scheme;

import com.google.gson.*;
import com.xioyim.titlepro.data.TitleData;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Saves and loads TitlePro scheme JSON files.
 * Files are stored in: config/titlepro/schemes/<name>.json
 */
public class TitleProScheme {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Returns (and creates if absent) the schemes directory. */
    public static Path getSchemesDir() {
        Path dir = FMLPaths.CONFIGDIR.get().resolve("titlepro").resolve("schemes");
        try { Files.createDirectories(dir); } catch (IOException ignored) {}
        return dir;
    }

    /**
     * Save a TitleData + extra-command info to {@code <name>.json}.
     * The § section-sign character is written as the {@code \u00a7} escape
     * so the file is human-readable and round-trips correctly.
     */
    public static void save(String name,
                            TitleData data,
                            boolean extraCmdEnabled,
                            String  extraCmd) throws IOException {
        JsonObject obj = new JsonObject();

        // text component — embed as a JSON element (not a double-encoded string)
        String rawTextJson = Component.Serializer.toJson(data.text);
        obj.add("text", JsonParser.parseString(rawTextJson));

        obj.addProperty("offsetX",        data.offsetX);
        obj.addProperty("offsetY",        data.offsetY);
        obj.addProperty("scale",          data.scale);
        obj.addProperty("stay",           data.stay);
        obj.addProperty("fadeIn",         data.fadeIn);
        obj.addProperty("fadeOut",        data.fadeOut);
        obj.addProperty("spacing",        data.spacing);
        obj.addProperty("stackUp",        data.stackUp);
        obj.addProperty("bgType",         data.bgType);
        obj.addProperty("bgColor",        String.format("#%06X", data.bgColor & 0xFFFFFF));
        obj.addProperty("bgAlpha",        data.bgAlpha);
        obj.addProperty("bgPaddingX",     data.bgPaddingX);
        obj.addProperty("bgPaddingY",     data.bgPaddingY);
        obj.addProperty("bgOffsetY",      data.bgOffsetY);
        obj.addProperty("shadowOffsetX",  data.shadowOffsetX);
        obj.addProperty("shadowOffsetY",  data.shadowOffsetY);
        obj.addProperty("exitSlide",      data.exitSlide);
        obj.addProperty("exitSpeed",      data.exitSpeed);
        obj.addProperty("textAlign",      data.textAlign);
        obj.addProperty("extraCmdEnabled", extraCmdEnabled);
        obj.addProperty("extraCmd",       extraCmd != null ? extraCmd : "");

        // Serialize; replace literal § with the \u00a7 Unicode escape for readability
        String json = GSON.toJson(obj);
        json = json.replace("§", "\\u00a7");

        String fileName = name.endsWith(".json") ? name : (name + ".json");
        Files.writeString(getSchemesDir().resolve(fileName), json, StandardCharsets.UTF_8);
    }

    /**
     * Load a scheme from {@code <name>.json} and return a {@link SchemeData}.
     * @throws IOException if the file cannot be read or parsed.
     */
    public static SchemeData load(String name) throws IOException {
        String fileName = name.endsWith(".json") ? name : (name + ".json");
        String content  = Files.readString(getSchemesDir().resolve(fileName), StandardCharsets.UTF_8);
        JsonObject obj  = JsonParser.parseString(content).getAsJsonObject();

        TitleData d = new TitleData();

        if (obj.has("text")) {
            try {
                d.text = Component.Serializer.fromJson(GSON.toJson(obj.get("text")));
                if (d.text == null) d.text = Component.literal("");
            } catch (Exception e) {
                d.text = Component.literal(obj.get("text").getAsString());
            }
        }

        d.offsetX       = gi(obj, "offsetX",       0);
        d.offsetY       = gi(obj, "offsetY",       0);
        d.scale         = gf(obj, "scale",         1f);
        d.stay          = gi(obj, "stay",          5000);
        d.fadeIn        = gi(obj, "fadeIn",        0);
        d.fadeOut       = gi(obj, "fadeOut",       1000);
        d.spacing       = gi(obj, "spacing",       14);
        d.stackUp       = gb(obj, "stackUp",       false);
        d.bgType        = gi(obj, "bgType",        1);
        if (obj.has("bgColor")) d.bgColor = parseHexColor(obj.get("bgColor").getAsString());
        d.bgAlpha       = gi(obj, "bgAlpha",       127);
        d.bgPaddingX    = gi(obj, "bgPaddingX",    3);
        d.bgPaddingY    = gi(obj, "bgPaddingY",    1);
        d.bgOffsetY     = gf(obj, "bgOffsetY",     -0.3f);
        d.shadowOffsetX = Math.max(0f, gf(obj, "shadowOffsetX", 0.4f));
        d.shadowOffsetY = Math.max(0f, gf(obj, "shadowOffsetY", 0.4f));
        d.exitSlide     = gb(obj, "exitSlide",     true);
        d.exitSpeed     = gf(obj, "exitSpeed",     1f);
        d.textAlign     = gi(obj, "textAlign",     1);

        boolean extraEnabled = gb(obj, "extraCmdEnabled", false);
        String  extraCmd     = gs(obj, "extraCmd",         "");

        // Also store in TitleData so applyToFields() picks them up automatically
        d.extraCmdEnabled = extraEnabled;
        d.extraCmd        = extraCmd;

        return new SchemeData(d, extraEnabled, extraCmd);
    }

    /** List all scheme names (without .json extension), sorted alphabetically. */
    public static List<String> listNames() {
        List<String> names = new ArrayList<>();
        try (Stream<Path> s = Files.list(getSchemesDir())) {
            s.filter(p -> p.getFileName().toString().endsWith(".json"))
             .map(p -> { String fn = p.getFileName().toString(); return fn.substring(0, fn.length() - 5); })
             .sorted()
             .forEach(names::add);
        } catch (IOException ignored) {}
        return names;
    }

    // ── JSON helpers ──────────────────────────────────────────────────────────

    private static int     gi(JsonObject o, String k, int def)     { return o.has(k) ? o.get(k).getAsInt()     : def; }
    private static float   gf(JsonObject o, String k, float def)   { return o.has(k) ? o.get(k).getAsFloat()   : def; }
    private static boolean gb(JsonObject o, String k, boolean def) { return o.has(k) ? o.get(k).getAsBoolean() : def; }
    private static String  gs(JsonObject o, String k, String def)  { return o.has(k) ? o.get(k).getAsString()  : def; }

    private static int parseHexColor(String s) {
        s = s.trim();
        if (s.startsWith("#"))                    s = s.substring(1);
        else if (s.toLowerCase().startsWith("0x")) s = s.substring(2);
        try { return (int)(Long.parseLong(s, 16) & 0xFFFFFF); }
        catch (NumberFormatException e) { return 0; }
    }

    // ── SchemeData ────────────────────────────────────────────────────────────

    public static class SchemeData {
        public final TitleData data;
        public final boolean   extraCmdEnabled;
        public final String    extraCmd;

        public SchemeData(TitleData data, boolean extraCmdEnabled, String extraCmd) {
            this.data            = data;
            this.extraCmdEnabled = extraCmdEnabled;
            this.extraCmd        = extraCmd;
        }
    }
}
