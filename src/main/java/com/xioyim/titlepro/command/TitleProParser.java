package com.xioyim.titlepro.command;

import com.google.gson.JsonParseException;
import com.xioyim.titlepro.data.TitleData;
import net.minecraft.network.chat.Component;

public class TitleProParser {

    public static class ParseResult {
        public final boolean valid;
        public final String error;
        public final TitleData data;

        private ParseResult(TitleData data) {
            this.valid = true;
            this.error = null;
            this.data = data;
        }

        private ParseResult(String error) {
            this.valid = false;
            this.error = error;
            this.data = null;
        }
    }

    /**
     * Parse command args string (everything after "/titlepro @s ").
     * Format: <text> [X] [Y] [scale] [stay_s] [fadeIn_s] [fadeOut_s] [spacing] [up|down]
     *         [#bgColor] [bgAlpha%] [bgPadX] [bgPadY] [bgOffsetY]
     *         [exitSlide:true/false] [exitSpeed]
     *         [bgType:shadow|bg] [shadowOffX] [shadowOffY]
     *         [textAlign:left|center|right]
     *         [extraCmdEnabled:true/false] [extraCmd...]
     */
    public static ParseResult parse(String input) {
        if (input == null || input.isBlank()) {
            return new ParseResult("Missing arguments");
        }
        input = input.trim();

        TitleData data = new TitleData();
        String rest;

        // --- Extract text token ---
        if (input.startsWith("{")) {
            // JSON component: find matching closing brace (depth-aware, ignoring braces in strings)
            int depth = 0;
            boolean inStr = false;
            boolean escaped = false;
            int end = -1;
            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                if (escaped) { escaped = false; continue; }
                if (c == '\\' && inStr) { escaped = true; continue; }
                if (c == '"') { inStr = !inStr; continue; }
                if (!inStr) {
                    if (c == '{') depth++;
                    else if (c == '}') {
                        depth--;
                        if (depth == 0) { end = i; break; }
                    }
                }
            }
            if (end == -1) return new ParseResult("Unclosed JSON object");
            String json = input.substring(0, end + 1);
            rest = input.substring(end + 1).trim();
            try {
                data.text = Component.Serializer.fromJson(json);
                if (data.text == null) return new ParseResult("Invalid JSON component");
            } catch (JsonParseException e) {
                return new ParseResult("JSON parse error: " + e.getMessage());
            }
        } else if (input.startsWith("\"")) {
            // Quoted string: find closing quote (handles \" escapes)
            int end = -1;
            for (int i = 1; i < input.length(); i++) {
                if (input.charAt(i) == '"' && input.charAt(i - 1) != '\\') {
                    end = i;
                    break;
                }
            }
            if (end == -1) return new ParseResult("Unclosed quoted string");
            // Try to parse as a JSON string component first
            String quoted = input.substring(0, end + 1); // includes the quotes
            rest = input.substring(end + 1).trim();
            try {
                Component fromJson = Component.Serializer.fromJson(quoted);
                data.text = (fromJson != null) ? fromJson
                        : Component.literal(input.substring(1, end).replace("\\\"", "\""));
            } catch (JsonParseException e) {
                data.text = Component.literal(input.substring(1, end).replace("\\\"", "\""));
            }
        } else {
            // Bare token until first space
            int space = input.indexOf(' ');
            if (space == -1) {
                data.text = Component.literal(input);
                rest = "";
            } else {
                data.text = Component.literal(input.substring(0, space));
                rest = input.substring(space + 1).trim();
            }
        }

        // --- Parse optional positional arguments ---
        String[] args = rest.isEmpty() ? new String[0] : rest.split("\\s+");

        try {
            // [0] X offset
            if (args.length > 0  && !args[0].isEmpty())  data.offsetX      = Integer.parseInt(args[0]);
            // [1] Y offset
            if (args.length > 1  && !args[1].isEmpty())  data.offsetY      = Integer.parseInt(args[1]);
            // [2] scale
            if (args.length > 2  && !args[2].isEmpty())  data.scale        = Math.max(0.1f, Float.parseFloat(args[2]));
            // [3] stay (seconds)
            if (args.length > 3  && !args[3].isEmpty())  data.stay         = Math.max(0, (int)(Float.parseFloat(args[3]) * 1000));
            // [4] fadeIn (seconds)
            if (args.length > 4  && !args[4].isEmpty())  data.fadeIn       = Math.max(0, (int)(Float.parseFloat(args[4]) * 1000));
            // [5] fadeOut (seconds)
            if (args.length > 5  && !args[5].isEmpty())  data.fadeOut      = Math.max(0, (int)(Float.parseFloat(args[5]) * 1000));
            // [6] spacing
            if (args.length > 6  && !args[6].isEmpty())  data.spacing      = Math.max(1, Integer.parseInt(args[6]));
            // [7] stack direction
            if (args.length > 7  && !args[7].isEmpty())  data.stackUp      = args[7].equalsIgnoreCase("up");
            // [8] bgColor
            if (args.length > 8  && !args[8].isEmpty())  data.bgColor      = parseColor(args[8]);
            // [9] bgAlpha (percentage 0-100)
            if (args.length > 9  && !args[9].isEmpty()) {
                int pct = Integer.parseInt(args[9]);
                data.bgAlpha = (int)(Math.max(0, Math.min(100, pct)) * 2.55f);
            }
            // [10] bgPaddingX
            if (args.length > 10 && !args[10].isEmpty()) data.bgPaddingX   = Math.max(0, Integer.parseInt(args[10]));
            // [11] bgPaddingY
            if (args.length > 11 && !args[11].isEmpty()) data.bgPaddingY   = Math.max(0, Integer.parseInt(args[11]));
            // [12] bgOffsetY
            if (args.length > 12 && !args[12].isEmpty()) data.bgOffsetY    = Float.parseFloat(args[12]);
            // [13] exitSlide
            if (args.length > 13 && !args[13].isEmpty()) data.exitSlide    = Boolean.parseBoolean(args[13]);
            // [14] exitSpeed
            if (args.length > 14 && !args[14].isEmpty()) data.exitSpeed    = Math.max(0.1f, Float.parseFloat(args[14]));
            // [15] bgType: "shadow" → 0, "bg" → 1 (or integer 0/1)
            if (args.length > 15 && !args[15].isEmpty()) {
                String bt = args[15].toLowerCase();
                if (bt.equals("shadow"))      data.bgType = 0;
                else if (bt.equals("bg"))     data.bgType = 1;
                else                          data.bgType = Math.max(0, Math.min(1, Integer.parseInt(bt)));
            }
            // [16] shadowOffsetX — clamped to >= 0 (rightward only)
            if (args.length > 16 && !args[16].isEmpty()) data.shadowOffsetX = Math.max(0f, Float.parseFloat(args[16]));
            // [17] shadowOffsetY — clamped to >= 0 (downward only)
            if (args.length > 17 && !args[17].isEmpty()) data.shadowOffsetY = Math.max(0f, Float.parseFloat(args[17]));
            // [18] textAlign: "left"→0, "center"→1, "right"→2, or integer 0/1/2
            if (args.length > 18 && !args[18].isEmpty()) {
                String ta = args[18].toLowerCase();
                if (ta.equals("left"))        data.textAlign = 0;
                else if (ta.equals("right"))  data.textAlign = 2;
                else if (ta.equals("center")) data.textAlign = 1;
                else data.textAlign = Math.max(0, Math.min(2, Integer.parseInt(ta)));
            }
            // [19] extraCmdEnabled: "true" / "false"
            if (args.length > 19 && !args[19].isEmpty()) data.extraCmdEnabled = Boolean.parseBoolean(args[19]);
            // [20+] extraCmd: remainder joined back with spaces
            if (args.length > 20) {
                StringBuilder sb = new StringBuilder(args[20]);
                for (int i = 21; i < args.length; i++) sb.append(' ').append(args[i]);
                data.extraCmd = sb.toString();
            }
        } catch (NumberFormatException e) {
            return new ParseResult("Invalid number: " + e.getMessage());
        }

        return new ParseResult(data);
    }

    /** Parse hex color string: "#FF0000", "0xFF0000", or "FF0000" */
    public static int parseColor(String s) {
        s = s.trim();
        if (s.startsWith("#")) s = s.substring(1);
        else if (s.toLowerCase().startsWith("0x")) s = s.substring(2);
        try {
            return (int)(Long.parseLong(s, 16) & 0xFFFFFF);
        } catch (NumberFormatException e) {
            return 0x000000;
        }
    }

    /** Build /titlepro command string from TitleData (includes @s selector). */
    public static String buildCommand(TitleData d) {
        String textStr;
        try {
            // Escape § → \u00a7 so the clipboard string is ASCII-safe and survives
            // Minecraft's chat-input character filter when pasted back as a command.
            textStr = Component.Serializer.toJson(d.text).replace("§", "\\u00a7");
        } catch (Exception e) {
            textStr = "\"" + d.text.getString().replace("§", "\\u00a7").replace("\"", "\\\"") + "\"";
        }
        int bgAlphaPct = Math.round(d.bgAlpha / 2.55f);
        String alignStr = d.textAlign == 0 ? "left" : d.textAlign == 2 ? "right" : "center";
        String base = String.format(
            "/titlepro @p %s %d %d %.2f %.2f %.2f %.2f %d %s #%06X %d %d %d %.2f %s %.1f %s %.2f %.2f %s",
            textStr,
            d.offsetX, d.offsetY, d.scale,
            d.stay    / 1000.0f,
            d.fadeIn  / 1000.0f,
            d.fadeOut / 1000.0f,
            d.spacing,
            d.stackUp ? "up" : "down",
            d.bgColor & 0xFFFFFF,
            bgAlphaPct,
            d.bgPaddingX,
            d.bgPaddingY,
            d.bgOffsetY,
            d.exitSlide ? "true" : "false",
            d.exitSpeed,
            d.bgType == 0 ? "shadow" : "bg",
            d.shadowOffsetX,
            d.shadowOffsetY,
            alignStr
        );
        // Append extra-command args only when there is something to say
        boolean hasExtra = d.extraCmdEnabled || (d.extraCmd != null && !d.extraCmd.isBlank());
        if (!hasExtra) return base;
        StringBuilder sb = new StringBuilder(base);
        sb.append(' ').append(d.extraCmdEnabled ? "true" : "false");
        if (d.extraCmd != null && !d.extraCmd.isBlank()) sb.append(' ').append(d.extraCmd.trim());
        return sb.toString();
    }
}
