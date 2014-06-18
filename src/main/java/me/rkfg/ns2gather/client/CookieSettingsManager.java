package me.rkfg.ns2gather.client;

import com.google.gwt.user.client.Cookies;

public class CookieSettingsManager {

    public static final String CHAT_VOLUME_COOKIE = "chat_volume";
    public static final String REMEMBER_STEAM_ID = "rememberSteamId";
    public static final String KICKED = "kicked";

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

    public void setStringCookie(String name, String value) {
        Cookies.setCookie(name, value);
    }

    public void setLongCookie(String name, Long value) {
        setStringCookie(name, value.toString());
    }

    public void removeCookie(String name) {
        Cookies.removeCookie(name);
    }
}
