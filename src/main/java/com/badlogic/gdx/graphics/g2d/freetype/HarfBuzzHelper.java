package com.badlogic.gdx.graphics.g2d.freetype;

import com.badlogic.gdx.graphics.text.harfbuzz.HarfBuzzUtil;

/**
 * To be used by {@link HarfBuzzUtil} internally.
 */
public class HarfBuzzHelper {
    public static long addressOf(FreeType.Face face) {
        return face.address;
    }
}
