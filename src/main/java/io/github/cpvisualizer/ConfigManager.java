package io.github.cpvisualizer;

import org.bukkit.Color;
import org.bukkit.configuration.file.FileConfiguration;
import java.time.ZoneId;
import java.util.Locale;

public record ConfigManager(int maxResults, int defaultTime, int maxTime, long cooldown,
                            int workers, int queue, int maxRadius, int blockScanLimit, int scanLimit, int historyPositions,
                            int historyRows, int timeout, int visible, int distance, int lifetime,
                            int recent, int medium, int old, Color green, Color yellow, Color gold,
                            Color red, ZoneId zone) {
    public static ConfigManager read(FileConfiguration c) {
        int def = TimeParser.seconds(c.getString("search.default-time", "24h"));
        int max = TimeParser.seconds(c.getString("search.max-time", "30d"));
        if (def > max) throw new IllegalArgumentException("default-time > max-time");
        int recent = TimeParser.seconds(c.getString("visualization.recent.max-age", "15m"));
        int medium = TimeParser.seconds(c.getString("visualization.medium.max-age", "1h"));
        int old = TimeParser.seconds(c.getString("visualization.old.max-age", "6h"));
        if (recent >= medium || medium >= old) throw new IllegalArgumentException("Age thresholds must increase strictly.");
        return new ConfigManager(number(c,"search.max-results",500,1,10000),def,max,
            number(c,"search.cooldown-ms",1000,0,600000), number(c,"search.concurrent-queries",2,1,8),
            number(c,"search.queued-queries",8,1,100),number(c,"search.max-radius",5000,1,100000),
            number(c,"search.block-scan-limit",2000,1,20000),number(c,"search.container-scan-limit",2000,1,20000),number(c,"search.history-position-limit",128,1,2000),
            number(c,"search.history-row-limit",1000,1,10000),number(c,"search.timeout-seconds",30,1,300),
            number(c,"visualization.max-visible-positions",100,1,500),number(c,"visualization.view-distance",96,8,256),
            number(c,"visualization.lifetime-seconds",300,5,3600),recent,medium,old,
            color(c,"recent","GREEN"),color(c,"medium","YELLOW"),color(c,"old","GOLD"),color(c,"very-old","RED"),
            ZoneId.of(c.getString("display-timezone","Europe/Moscow")));
    }
    private static int number(FileConfiguration c,String path,int fallback,int min,int max) {
        int n = c.getInt(path,fallback);
        if(n < min || n > max) throw new IllegalArgumentException(path + " must be " + min + ".." + max);
        return n;
    }
    private static Color color(FileConfiguration c,String section,String fallback) {
        String name = c.getString("visualization."+section+".color",fallback).toUpperCase(Locale.ROOT);
        return switch(name) {
            case "GREEN" -> Color.LIME; case "YELLOW" -> Color.YELLOW; case "GOLD","ORANGE" -> Color.ORANGE;
            case "RED" -> Color.RED; case "BLUE" -> Color.BLUE; case "WHITE" -> Color.WHITE;
            case "AQUA" -> Color.AQUA; case "PURPLE" -> Color.PURPLE;
            default -> { if(!name.matches("#[0-9A-F]{6}")) throw new IllegalArgumentException("Unknown color: "+name);
                yield Color.fromRGB(Integer.parseInt(name.substring(1),16)); }
        };
    }
    public Color ageColor(long timestamp) {
        long age = Math.max(0,(System.currentTimeMillis()-timestamp)/1000);
        return age < recent ? green : age < medium ? yellow : age < old ? gold : red;
    }
}
