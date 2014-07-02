package me.rkfg.ns2gather.client;

import java.util.Date;

import com.google.gwt.user.client.Cookies;

public class CookieSettingsManager {

    public static final String CHAT_VOLUME_COOKIE = "chat_volume";
    public static final String CHAT_MUTE_COOKIE = "chat_mute";
    public static final String REMEMBER_STEAM_ID = "rememberSteamId";
    public static final long COOKIE_AGE = 3650 * 24 * 3600;
    public static final String PLAYER_PANEL_COOKIE = "playerpanel_size";
    public static final String SERVER_PANEL_COOKIE = "serverpanel_size";
    public static final String CHAT_PANEL_COOKIE = "chatpanel_size";
    public static final String HEADER_PANEL_COOKIE = "headerpanel_size";
    public static final String CHAT_SYSTEM_PANEL_COOKIE = "chatsystempanel_size";

    public String getStringCookie(String name, String defaultValue) {
        String result = Cookies.getCookie(name);
        if (result == null) {
            return defaultValue;
        }
        return result;
    }

    public Long getLongCookie(String name, Long defaultValue) {
        String result = getStringCookie(name, null);
        if (result == null) {
            return defaultValue;
        }
        return Long.valueOf(result);

    }

    public boolean getBooleanCookie(String name, Boolean defaultValue) {
        return getStringCookie(name, defaultValue ? "1" : "0").equals("1");
    }

    public double getDoubleCookie(String name, Double defaultValue) {
        return Double.valueOf(getStringCookie(name, defaultValue.toString()));
    }

    public void setStringCookie(String name, String value) {
        Cookies.setCookie(name, value, new Date(System.currentTimeMillis() + COOKIE_AGE * 1000));
    }

    public void setLongCookie(String name, Long value) {
        setStringCookie(name, value.toString());
    }

    public void setBooleanCookie(String name, Boolean value) {
        setStringCookie(name, value ? "1" : "0");
    }

    public void removeCookie(String name) {
        Cookies.removeCookie(name);
    }

    public void setDoubleCookie(String name, Double value) {
        setStringCookie(name, value.toString());
    }

}
