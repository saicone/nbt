package com.saicone.nbt;

import com.saicone.nbt.util.TagPalette;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TagPaletteTest {

    private static String COLORED = "§f{§bbyte§f: §c1§f, §bshort§f: §62§cs§f, §bint§f: §63§f, §blong§f: §64§cl§f, §bfloat§f: §65.0§cf§f, §bdouble§f: §66.0§cd§f, §bbyte array§f: §f[§cB§f; §61§cB§f, §62§cB§f, §63§cB§f, §64§cB§f]§f, §bstring§f: §f\"§atest123§f\"§f, §binteger list§f: §f[§61§f, §62§f, §63§f, §64§f]§f, §barray list§f: §f[§f[§cI§f; §61§f]§f, §f[§cI§f; §62§f]§f, §f[§cI§f; §63§f]§f, §f[§cI§f; §64§f]§f]§f, §blist list§f: §f[§f[§f\"§a1§f\"§f]§f, §f[§f\"§a2§f\"§f]§f, §f[§f\"§a3§f\"§f]§f, §f[§f\"§a4§f\"§f]§f]§f, §bcompound list§f: §f[§f{§btest§f: §f\"§aasd§f\"§f}§f, §f{§bnumber§f: §61234§f}§f, §f{§blist§f: §f[§61234§cs§f]§f}§f]§f, §bcompound§f: §f{§btest§f: §f{§blist§f: §f[§61234§cs§f]§f}§f}§f, §bint array§f: §f[§cI§f; §61§f, §62§f, §63§f, §64§f]§f, §blong array§f: §f[§cL§f; §61§cL§f, §62§cL§f, §63§cL§f, §64§cL§f]§f}§r";

    @Test
    public void testDefault() {
        assertEquals(COLORED, TagPalette.DEFAULT.color(TagObjects.MAP, null));
    }
}
