package bot.aperture.utils;

import net.md_5.bungee.api.ChatColor;

public class StringUtils {

    /**
     * Color a message using & color codes
     *
     * @param message message
     * @return colored message
     */
    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

}