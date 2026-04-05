package dev.stacksizeconf;

/**
 * Which brightness value the in-world light overlay shows.
 */
public enum LightOverlayBrightnessMode {
    /** Torches, glowstone, etc. Night outdoors with no sources is 0. */
    BLOCK,
    /** Vanilla {@code getRawBrightness} (max of block and sky after {@code getSkyDarken()}). */
    COMBINED
}
