package com.rtsoft.growtopia;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Manages spoofed device identifiers (MAC, GID, RID) and OpenGL / display
 * settings.  Values are persisted in SharedPreferences and generated once
 * on first launch — they are never automatically re-randomized so the engine
 * always reports consistent hardware IDs across sessions.
 */
public class DeviceSpoofer {

    private static final String PREFS_NAME      = "device_spoof_prefs";
    private static final String KEY_INIT        = "initialized";
    private static final String KEY_MAC         = "mac_address";
    private static final String KEY_GID         = "gid";
    private static final String KEY_RID         = "rid";
    private static final String KEY_OGL_ENABLED = "spoof_opengl";
    private static final String KEY_OGL_VERSION = "opengl_version";
    private static final String KEY_OGL_EXT     = "opengl_extensions";
    private static final String KEY_FULLSCREEN  = "fullscreen_launch";

    public static final String DEFAULT_OGL_VERSION =
        "OpenGL ES 2.0";
    public static final String DEFAULT_OGL_EXTENSIONS =
        "GL_OES_rgb8_rgba8\nGL_OES_depth24\nGL_OES_vertex_half_float\n"
        + "GL_OES_texture_float\nGL_OES_element_index_uint\n"
        + "GL_OES_mapbuffer\nGL_OES_compressed_ETC1_RGB8_texture";

    private final SharedPreferences prefs;

    public DeviceSpoofer(Context context) {
        prefs = context.getApplicationContext()
                       .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        ensureInitialized();
    }

    // -----------------------------------------------------------------------
    // One-time initialization
    // -----------------------------------------------------------------------

    /** Generates and saves default values exactly once. Never called again. */
    private void ensureInitialized() {
        if (prefs.getBoolean(KEY_INIT, false)) return;
        Log.d("DeviceSpoofer", "First launch — generating device identifiers");
        prefs.edit()
             .putString(KEY_MAC,         generateMac())
             .putString(KEY_GID,         generateGid())
             .putString(KEY_RID,         generateRid())
             .putBoolean(KEY_OGL_ENABLED, false)
             .putString(KEY_OGL_VERSION,  DEFAULT_OGL_VERSION)
             .putString(KEY_OGL_EXT,      DEFAULT_OGL_EXTENSIONS)
             .putBoolean(KEY_FULLSCREEN,  false)
             .putBoolean(KEY_INIT,        true)
             .apply();
    }

    // -----------------------------------------------------------------------
    // Getters
    // -----------------------------------------------------------------------

    public String  getMac()               { return prefs.getString(KEY_MAC, ""); }
    public String  getGid()               { return prefs.getString(KEY_GID, ""); }
    public String  getRid()               { return prefs.getString(KEY_RID, ""); }
    public boolean isSpoofOpenGL()        { return prefs.getBoolean(KEY_OGL_ENABLED, false); }
    public String  getOpenGLVersion()     { return prefs.getString(KEY_OGL_VERSION, DEFAULT_OGL_VERSION); }
    public String  getOpenGLExtensions()  { return prefs.getString(KEY_OGL_EXT, DEFAULT_OGL_EXTENSIONS); }
    public boolean isFullscreen()         { return prefs.getBoolean(KEY_FULLSCREEN, false); }

    // -----------------------------------------------------------------------
    // Setters
    // -----------------------------------------------------------------------

    public void setMac(String v)              { prefs.edit().putString(KEY_MAC, v.toUpperCase().trim()).apply(); }
    public void setGid(String v)              { prefs.edit().putString(KEY_GID, v.trim()).apply(); }
    public void setRid(String v)              { prefs.edit().putString(KEY_RID, v.toUpperCase().trim()).apply(); }
    public void setSpoofOpenGL(boolean b)     { prefs.edit().putBoolean(KEY_OGL_ENABLED, b).apply(); }
    public void setOpenGLVersion(String v)    { prefs.edit().putString(KEY_OGL_VERSION, v.trim()).apply(); }
    public void setOpenGLExtensions(String v) { prefs.edit().putString(KEY_OGL_EXT, v.trim()).apply(); }
    public void setFullscreen(boolean b)      { prefs.edit().putBoolean(KEY_FULLSCREEN, b).apply(); }

    // -----------------------------------------------------------------------
    // Generators  (static — callable without an instance)
    // -----------------------------------------------------------------------

    /**
     * Generates a locally-administered unicast MAC address.
     * Format: XX:XX:XX:XX:XX:XX (uppercase hex).
     */
    public static String generateMac() {
        byte[] b = new byte[6];
        new SecureRandom().nextBytes(b);
        b[0] = (byte) ((b[0] & 0xFE) | 0x02); // locally administered, unicast
        return String.format("%02X:%02X:%02X:%02X:%02X:%02X",
            b[0] & 0xFF, b[1] & 0xFF, b[2] & 0xFF,
            b[3] & 0xFF, b[4] & 0xFF, b[5] & 0xFF);
    }

    /** Generates a random UUID v4. Format: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx */
    public static String generateGid() {
        return UUID.randomUUID().toString();
    }

    /** Generates a random 32-character uppercase hex string. */
    public static String generateRid() {
        byte[] b = new byte[16];
        new SecureRandom().nextBytes(b);
        StringBuilder sb = new StringBuilder(32);
        for (byte x : b) sb.append(String.format("%02X", x & 0xFF));
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // Validators
    // -----------------------------------------------------------------------

    public static boolean isValidMac(String v) {
        return v != null && v.matches("^[0-9A-Fa-f]{2}(:[0-9A-Fa-f]{2}){5}$");
    }

    public static boolean isValidGid(String v) {
        return v != null && v.matches(
            "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");
    }

    public static boolean isValidRid(String v) {
        return v != null && v.matches("^[0-9A-Fa-f]{32}$");
    }
}
