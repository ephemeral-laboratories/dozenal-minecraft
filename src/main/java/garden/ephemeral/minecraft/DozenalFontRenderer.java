package garden.ephemeral.minecraft;

import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nonnull;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Custom font renderer adding support for the two additional glyphs I needed.
 * Extends {@link FontRenderer} but ends up also duplicating most or all of its code because some
 * of the logic I wanted to change was hidden in private methods.
 */
@SideOnly(Side.CLIENT)
public class DozenalFontRenderer extends FontRenderer implements IResourceManagerReloadListener {
    private static final String DEFAULT_CHARS =
            "\u00C0\u00C1\u00C2\u00C8\u00CA\u00CB\u00CD\u00D3\u00D4\u00D5\u00DA\u00DF\u00E3\u00F5\u011F\u0130" +
                    "\u0131\u0152\u0153\u015E\u015F\u0174\u0175\u017E\u0207\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
                    " !\"#$%&'()*+,-./" +
                    "0123456789:;<=>?" +
                    "@ABCDEFGHIJKLMNO" +
                    "PQRSTUVWXYZ[\\]^_" +
                    "`abcdefghijklmno" +
                    "pqrstuvwxyz{|}~\u0000" +
                    "\u00C7\u00FC\u00E9\u00E2\u00E4\u00E0\u00E5\u00E7\u00EA\u00EB\u00E8\u00EF\u00EE\u00EC\u00C4\u00C5" +
                    "\u00C9\u00E6\u00C6\u00F4\u00F6\u00F2\u00FB\u00F9\u00FF\u00D6\u00DC\u00F8\u00A3\u00D8\u00D7\u0192" +
                    "\u00E1\u00ED\u00F3\u00FA\u00F1\u00D1\u00AA\u00BA\u00BF\u00AE\u00AC\u00BD\u00BC\u00A1\u00AB\u00BB" +
                    "\u2591\u2592\u2593\u2502\u2524\u2561\u2562\u2556\u2555\u2563\u2551\u2557\u255D\u255C\u255B\u2510" +
                    "\u2514\u2534\u252C\u251C\u2500\u253C\u255E\u255F\u255A\u2554\u2569\u2566\u2560\u2550\u256C\u2567" +
                    "\u2568\u2564\u2565\u2559\u2558\u2552\u2553\u256B\u256A\u2518\u250C\u2588\u2584\u258C\u2590\u2580" +
                    "\u03B1\u03B2\u0393\u03C0\u03A3\u03C3\u03BC\u03C4\u03A6\u0398\u03A9\u03B4\u221E\u2205\u2208\u2229" +
                    "\u2261\u00B1\u2265\u2264\u2320\u2321\u00F7\u2248\u00B0\u2219\u00B7\u221A\u207F\u00B2\u25A0\u0000";
    private static final String DOZENAL_CHARS =
            "\u218A\u218B";
    private static final ResourceLocation[] UNICODE_PAGE_LOCATIONS = new ResourceLocation[256];
    protected final int[] charWidth = new int[256];
    private final int[] dozenalCharWidth = {6, 6};
    public int FONT_HEIGHT = 9;
    public Random fontRandom = new Random();
    protected final byte[] glyphWidth = new byte[65536];
    private final int[] colorCode = new int[32];
    protected final ResourceLocation locationFontTexture;
    protected final ResourceLocation dozenalFontTexture;
    protected float posX;
    protected float posY;
    private boolean unicodeFlag;
    private boolean bidiFlag;
    private float red;
    private float blue;
    private float green;
    private float alpha;
    private int textColor;
    private boolean randomStyle;
    private boolean boldStyle;
    private boolean italicStyle;
    private boolean underlineStyle;
    private boolean strikethroughStyle;

    public DozenalFontRenderer(GameSettings gameSettings,
                               ResourceLocation defaultResourceLocation,
                               ResourceLocation dozenalResourceLocation,
                               TextureManager renderEngine,
                               boolean unicodeFlag) {

        // renderEngine in particular we don't keep a reference to because bindTexture
        // gets called at construction-time and we get an NPE because we haven't kept
        // a reference to it yet.
        super(gameSettings, defaultResourceLocation, renderEngine, unicodeFlag);

        this.locationFontTexture = defaultResourceLocation;
        this.dozenalFontTexture = dozenalResourceLocation;
        this.unicodeFlag = unicodeFlag;

        // FontRenderer does this on construction but I found I get an NPE if I do the same.
        this.bindTexture(this.locationFontTexture);
        this.bindTexture(this.dozenalFontTexture);

        for (int i = 0; i < 32; ++i) {
            int j = (i >> 3 & 1) * 85;
            int k = (i >> 2 & 1) * 170 + j;
            int l = (i >> 1 & 1) * 170 + j;
            int i1 = (i >> 0 & 1) * 170 + j;
            if (i == 6) {
                k += 85;
            }

            if (gameSettings.anaglyph) {
                int j1 = (k * 30 + l * 59 + i1 * 11) / 100;
                int k1 = (k * 30 + l * 70) / 100;
                int l1 = (k * 30 + i1 * 70) / 100;
                k = j1;
                l = k1;
                i1 = l1;
            }

            if (i >= 16) {
                k /= 4;
                l /= 4;
                i1 /= 4;
            }

            this.colorCode[i] = (k & 255) << 16 | (l & 255) << 8 | i1 & 255;
        }

        this.readGlyphSizes();
    }

    @Override
    public void onResourceManagerReload(@Nonnull IResourceManager resourceManager) {
        this.readFontTexture();
        this.readGlyphSizes();
    }

    private void readFontTexture() {
        IResource iresource = null;

        BufferedImage bufferedimage;
        try {
            iresource = this.getResource(this.locationFontTexture);
            bufferedimage = TextureUtil.readBufferedImage(iresource.getInputStream());
        } catch (IOException var20) {
            throw new RuntimeException(var20);
        } finally {
            IOUtils.closeQuietly(iresource);
        }

        int lvt_3_2_ = bufferedimage.getWidth();
        int lvt_4_1_ = bufferedimage.getHeight();
        int[] lvt_5_1_ = new int[lvt_3_2_ * lvt_4_1_];
        bufferedimage.getRGB(0, 0, lvt_3_2_, lvt_4_1_, lvt_5_1_, 0, lvt_3_2_);
        int lvt_6_1_ = lvt_4_1_ / 16;
        int lvt_7_1_ = lvt_3_2_ / 16;
        float lvt_9_1_ = 8.0F / (float) lvt_7_1_;

        for (int lvt_10_1_ = 0; lvt_10_1_ < 256; ++lvt_10_1_) {
            int j1 = lvt_10_1_ % 16;
            int k1 = lvt_10_1_ / 16;
            if (lvt_10_1_ == 32) {
                this.charWidth[lvt_10_1_] = 4;
            }

            int l1;
            for (l1 = lvt_7_1_ - 1; l1 >= 0; --l1) {
                int i2 = j1 * lvt_7_1_ + l1;
                boolean flag1 = true;

                for (int j2 = 0; j2 < lvt_6_1_; ++j2) {
                    int k2 = (k1 * lvt_7_1_ + j2) * lvt_3_2_;
                    if ((lvt_5_1_[i2 + k2] >> 24 & 255) != 0) {
                        flag1 = false;
                        break;
                    }
                }

                if (!flag1) {
                    break;
                }
            }

            ++l1;
            this.charWidth[lvt_10_1_] = (int) (0.5D + (double) ((float) l1 * lvt_9_1_)) + 1;
        }

    }

    private void readGlyphSizes() {
        IResource iresource = null;

        try {
            iresource = this.getResource(new ResourceLocation("font/glyph_sizes.bin"));
            new DataInputStream(iresource.getInputStream()).readFully(this.glyphWidth);
        } catch (IOException var6) {
            throw new RuntimeException(var6);
        } finally {
            IOUtils.closeQuietly(iresource);
        }
    }

    private float renderChar(char ch, boolean italicStyle) {
        if (ch == 160) {
            return 4.0F;
        } else if (ch == ' ') {
            return 4.0F;
        } else {
            if (!this.unicodeFlag) {
                int i = DEFAULT_CHARS.indexOf(ch);
                if (i != -1) {
                    return this.renderDefaultChar(i, italicStyle);
                }
                i = DOZENAL_CHARS.indexOf(ch);
                if (i != -1) {
                    return this.renderDozenalChar(i, italicStyle);
                }
            }
            return this.renderUnicodeChar(ch, italicStyle);
        }
    }

    @Override
    protected float renderDefaultChar(int code, boolean italicStyle) {
        int i = code % 16 * 8;
        int j = code / 16 * 8;
        int k = italicStyle ? 1 : 0;
        this.bindTexture(this.locationFontTexture);
        int l = this.charWidth[code];
        float f = (float) l - 0.01F;
        GlStateManager.glBegin(5);
        GlStateManager.glTexCoord2f((float) i / 128.0F, (float) j / 128.0F);
        GlStateManager.glVertex3f(this.posX + (float) k, this.posY, 0.0F);
        GlStateManager.glTexCoord2f((float) i / 128.0F, ((float) j + 7.99F) / 128.0F);
        GlStateManager.glVertex3f(this.posX - (float) k, this.posY + 7.99F, 0.0F);
        GlStateManager.glTexCoord2f(((float) i + f - 1.0F) / 128.0F, (float) j / 128.0F);
        GlStateManager.glVertex3f(this.posX + f - 1.0F + (float) k, this.posY, 0.0F);
        GlStateManager.glTexCoord2f(((float) i + f - 1.0F) / 128.0F, ((float) j + 7.99F) / 128.0F);
        GlStateManager.glVertex3f(this.posX + f - 1.0F - (float) k, this.posY + 7.99F, 0.0F);
        GlStateManager.glEnd();
        return (float) l;
    }

    // Identical to renderDefaultChar except for the texture used.
    private float renderDozenalChar(int code, boolean italicStyle) {
        int i = code % 16 * 8;
        int j = code / 16 * 8;
        int k = italicStyle ? 1 : 0;
        this.bindTexture(this.dozenalFontTexture);
        int l = this.dozenalCharWidth[code];
        float f = (float) l - 0.01F;
        GlStateManager.glBegin(5);
        GlStateManager.glTexCoord2f((float) i / 128.0F, (float) j / 128.0F);
        GlStateManager.glVertex3f(this.posX + (float) k, this.posY, 0.0F);
        GlStateManager.glTexCoord2f((float) i / 128.0F, ((float) j + 7.99F) / 128.0F);
        GlStateManager.glVertex3f(this.posX - (float) k, this.posY + 7.99F, 0.0F);
        GlStateManager.glTexCoord2f(((float) i + f - 1.0F) / 128.0F, (float) j / 128.0F);
        GlStateManager.glVertex3f(this.posX + f - 1.0F + (float) k, this.posY, 0.0F);
        GlStateManager.glTexCoord2f(((float) i + f - 1.0F) / 128.0F, ((float) j + 7.99F) / 128.0F);
        GlStateManager.glVertex3f(this.posX + f - 1.0F - (float) k, this.posY + 7.99F, 0.0F);
        GlStateManager.glEnd();
        return (float) l;
    }

    private ResourceLocation getUnicodePageLocation(int pageIndex) {
        if (UNICODE_PAGE_LOCATIONS[pageIndex] == null) {
            UNICODE_PAGE_LOCATIONS[pageIndex] = new ResourceLocation(String.format("textures/font/unicode_page_%02x.png", pageIndex));
        }

        return UNICODE_PAGE_LOCATIONS[pageIndex];
    }

    private void loadGlyphTexture(int pageIndex) {
        this.bindTexture(this.getUnicodePageLocation(pageIndex));
    }

    @Override
    protected float renderUnicodeChar(char ch, boolean italicStyle) {
        int i = this.glyphWidth[ch] & 255;
        if (i == 0) {
            return 0.0F;
        } else {
            int j = ch / 256;
            this.loadGlyphTexture(j);
            int k = i >>> 4;
            int l = i & 15;
            float f = (float) k;
            float f1 = (float) (l + 1);
            float f2 = (float) (ch % 16 * 16) + f;
            float f3 = (float) ((ch & 255) / 16 * 16);
            float f4 = f1 - f - 0.02F;
            float f5 = italicStyle ? 1.0F : 0.0F;
            GlStateManager.glBegin(5);
            GlStateManager.glTexCoord2f(f2 / 256.0F, f3 / 256.0F);
            GlStateManager.glVertex3f(this.posX + f5, this.posY, 0.0F);
            GlStateManager.glTexCoord2f(f2 / 256.0F, (f3 + 15.98F) / 256.0F);
            GlStateManager.glVertex3f(this.posX - f5, this.posY + 7.99F, 0.0F);
            GlStateManager.glTexCoord2f((f2 + f4) / 256.0F, f3 / 256.0F);
            GlStateManager.glVertex3f(this.posX + f4 / 2.0F + f5, this.posY, 0.0F);
            GlStateManager.glTexCoord2f((f2 + f4) / 256.0F, (f3 + 15.98F) / 256.0F);
            GlStateManager.glVertex3f(this.posX + f4 / 2.0F - f5, this.posY + 7.99F, 0.0F);
            GlStateManager.glEnd();
            return (f1 - f) / 2.0F + 1.0F;
        }
    }

    @Override
    public int drawStringWithShadow(@Nonnull String string, float x, float y, int textColor) {
        return this.drawString(string, x, y, textColor, true);
    }

    @Override
    public int drawString(@Nonnull String string, int x, int y, int textColor) {
        return this.drawString(string, (float) x, (float) y, textColor, false);
    }

    @Override
    public int drawString(@Nonnull String string, float x, float y, int textColor, boolean withShadow) {
        this.enableAlpha();
        this.resetStyles();
        int i;
        if (withShadow) {
            i = this.renderString(string, x + 1.0F, y + 1.0F, textColor, true);
            i = Math.max(i, this.renderString(string, x, y, textColor, false));
        } else {
            i = this.renderString(string, x, y, textColor, false);
        }

        return i;
    }

    private String bidiReorder(String string) {
        try {
            Bidi bidi = new Bidi((new ArabicShaping(8)).shape(string), 127);
            bidi.setReorderingMode(0);
            return bidi.writeReordered(2);
        } catch (ArabicShapingException var3) {
            return string;
        }
    }

    private void resetStyles() {
        this.randomStyle = false;
        this.boldStyle = false;
        this.italicStyle = false;
        this.underlineStyle = false;
        this.strikethroughStyle = false;
    }

    private void renderStringAtPos(String string, boolean shadow) {
        for (int i = 0; i < string.length(); ++i) {
            char c0 = string.charAt(i);
            int i1;
            int j1;
            if (c0 == 167 && i + 1 < string.length()) {
                i1 = "0123456789abcdefklmnor".indexOf(String.valueOf(string.charAt(i + 1)).toLowerCase(Locale.ROOT).charAt(0));
                if (i1 < 16) {
                    this.randomStyle = false;
                    this.boldStyle = false;
                    this.strikethroughStyle = false;
                    this.underlineStyle = false;
                    this.italicStyle = false;
                    if (i1 < 0) {
                        i1 = 15;
                    }

                    if (shadow) {
                        i1 += 16;
                    }

                    j1 = this.colorCode[i1];
                    this.textColor = j1;
                    this.setColor((float) (j1 >> 16) / 255.0F, (float) (j1 >> 8 & 255) / 255.0F, (float) (j1 & 255) / 255.0F, this.alpha);
                } else if (i1 == 16) {
                    this.randomStyle = true;
                } else if (i1 == 17) {
                    this.boldStyle = true;
                } else if (i1 == 18) {
                    this.strikethroughStyle = true;
                } else if (i1 == 19) {
                    this.underlineStyle = true;
                } else if (i1 == 20) {
                    this.italicStyle = true;
                } else { // i1 == 21
                    this.randomStyle = false;
                    this.boldStyle = false;
                    this.strikethroughStyle = false;
                    this.underlineStyle = false;
                    this.italicStyle = false;
                    this.setColor(this.red, this.blue, this.green, this.alpha);
                }

                ++i;
            } else {
                // XXX: Usage of DEFAULT_CHARS here, not sure whether I need to customise.
                i1 = DEFAULT_CHARS.indexOf(c0);
                if (this.randomStyle && i1 != -1) {
                    j1 = this.getCharWidth(c0);

                    char c1;
                    do {
                        i1 = this.fontRandom.nextInt(DEFAULT_CHARS.length());
                        c1 = DEFAULT_CHARS.charAt(i1);
                    } while (j1 != this.getCharWidth(c1));

                    c0 = c1;
                }
                int i2 = DOZENAL_CHARS.indexOf(c0);

                // Alignment fix for dozenal shadow when in non-Unicode rendering
                float f1 = ((i1 != -1 || i2 != -1) && !this.unicodeFlag) ? 1.0F : 0.5F;
                boolean flag = (c0 == 0 || (i1 == -1 && i2 == -1) || this.unicodeFlag) && shadow;
                if (flag) {
                    this.posX -= f1;
                    this.posY -= f1;
                }

                float f = this.renderChar(c0, this.italicStyle);
                if (flag) {
                    this.posX += f1;
                    this.posY += f1;
                }

                if (this.boldStyle) {
                    this.posX += f1;
                    if (flag) {
                        this.posX -= f1;
                        this.posY -= f1;
                    }

                    this.renderChar(c0, this.italicStyle);
                    this.posX -= f1;
                    if (flag) {
                        this.posX += f1;
                        this.posY += f1;
                    }

                    ++f;
                }

                this.doDraw(f);
            }
        }

    }

    @Override
    protected void doDraw(float xOffset) {
        Tessellator tessellator1;
        BufferBuilder bufferbuilder1;
        if (this.strikethroughStyle) {
            tessellator1 = Tessellator.getInstance();
            bufferbuilder1 = tessellator1.getBuffer();
            GlStateManager.disableTexture2D();
            bufferbuilder1.begin(7, DefaultVertexFormats.POSITION);
            bufferbuilder1.pos(this.posX, this.posY + (float) (this.FONT_HEIGHT / 2), 0.0D).endVertex();
            bufferbuilder1.pos(this.posX + xOffset, this.posY + (float) (this.FONT_HEIGHT / 2), 0.0D).endVertex();
            bufferbuilder1.pos(this.posX + xOffset, this.posY + (float) (this.FONT_HEIGHT / 2) - 1.0F, 0.0D).endVertex();
            bufferbuilder1.pos(this.posX, this.posY + (float) (this.FONT_HEIGHT / 2) - 1.0F, 0.0D).endVertex();
            tessellator1.draw();
            GlStateManager.enableTexture2D();
        }

        if (this.underlineStyle) {
            tessellator1 = Tessellator.getInstance();
            bufferbuilder1 = tessellator1.getBuffer();
            GlStateManager.disableTexture2D();
            bufferbuilder1.begin(7, DefaultVertexFormats.POSITION);
            int l = this.underlineStyle ? -1 : 0;
            bufferbuilder1.pos(this.posX + (float) l, this.posY + (float) this.FONT_HEIGHT, 0.0D).endVertex();
            bufferbuilder1.pos(this.posX + xOffset, this.posY + (float) this.FONT_HEIGHT, 0.0D).endVertex();
            bufferbuilder1.pos(this.posX + xOffset, this.posY + (float) this.FONT_HEIGHT - 1.0F, 0.0D).endVertex();
            bufferbuilder1.pos(this.posX + (float) l, this.posY + (float) this.FONT_HEIGHT - 1.0F, 0.0D).endVertex();
            tessellator1.draw();
            GlStateManager.enableTexture2D();
        }

        this.posX += (float) ((int) xOffset);
    }

    private void renderStringAligned(String string, int x, int y, int width, int textColor) {
        if (this.bidiFlag) {
            int i = this.getStringWidth(this.bidiReorder(string));
            x = x + width - i;
        }

        this.renderString(string, (float) x, (float) y, textColor, false);
    }

    private int renderString(String string, float x, float y, int textColor, boolean shadow) {
        if (string == null) {
            return 0;
        } else {
            if (this.bidiFlag) {
                string = this.bidiReorder(string);
            }

            if ((textColor & -67108864) == 0) {
                textColor |= -16777216;
            }

            if (shadow) {
                textColor = (textColor & 16579836) >> 2 | textColor & -16777216;
            }

            this.red = (float) (textColor >> 16 & 255) / 255.0F;
            this.blue = (float) (textColor >> 8 & 255) / 255.0F;
            this.green = (float) (textColor & 255) / 255.0F;
            this.alpha = (float) (textColor >> 24 & 255) / 255.0F;
            this.setColor(this.red, this.blue, this.green, this.alpha);
            this.posX = x;
            this.posY = y;
            this.renderStringAtPos(string, shadow);
            return (int) this.posX;
        }
    }

    @Override
    public int getStringWidth(@Nonnull String string) {
        //noinspection ConstantConditions
        if (string == null) {
            return 0;
        } else {
            int i = 0;
            boolean flag = false;

            for (int j = 0; j < string.length(); ++j) {
                char c0 = string.charAt(j);
                int k = this.getCharWidth(c0);
                if (k < 0 && j < string.length() - 1) {
                    ++j;
                    c0 = string.charAt(j);
                    if (c0 != 'l' && c0 != 'L') {
                        if (c0 == 'r' || c0 == 'R') {
                            flag = false;
                        }
                    } else {
                        flag = true;
                    }

                    k = 0;
                }

                i += k;
                if (flag && k > 0) {
                    ++i;
                }
            }

            return i;
        }
    }

    @Override
    public int getCharWidth(char ch) {
        if (ch == 160) {
            return 4;
        } else if (ch == 167) {
            return -1;
        } else if (ch == ' ') {
            return 4;
        } else {
            int i1 = DEFAULT_CHARS.indexOf(ch);
            int i2 = DOZENAL_CHARS.indexOf(ch);
            if (ch > 0 && i1 != -1 && !this.unicodeFlag) {
                return this.charWidth[i1];
            } else if (ch > 0 && i2 != -1 && !this.unicodeFlag) {
                return this.dozenalCharWidth[i2];
            } else if (this.glyphWidth[ch] != 0) {
                int j = this.glyphWidth[ch] & 255;
                int k = j >>> 4;
                int l = j & 15;
                ++l;
                return (l - k) / 2 + 1;
            } else {
                return 0;
            }
        }
    }

    @Override
    @Nonnull
    public String trimStringToWidth(@Nonnull String string, int width) {
        return this.trimStringToWidth(string, width, false);
    }

    @Override
    @Nonnull
    public String trimStringToWidth(@Nonnull String string, int width, boolean rightAligned) {
        StringBuilder stringbuilder = new StringBuilder();
        int i = 0;
        int j = rightAligned ? string.length() - 1 : 0;
        int k = rightAligned ? -1 : 1;
        boolean flag = false;
        boolean flag1 = false;

        for (int l = j; l >= 0 && l < string.length() && i < width; l += k) {
            char c0 = string.charAt(l);
            int i1 = this.getCharWidth(c0);
            if (flag) {
                flag = false;
                if (c0 != 'l' && c0 != 'L') {
                    if (c0 == 'r' || c0 == 'R') {
                        flag1 = false;
                    }
                } else {
                    flag1 = true;
                }
            } else if (i1 < 0) {
                flag = true;
            } else {
                i += i1;
                if (flag1) {
                    ++i;
                }
            }

            if (i > width) {
                break;
            }

            if (rightAligned) {
                stringbuilder.insert(0, c0);
            } else {
                stringbuilder.append(c0);
            }
        }

        return stringbuilder.toString();
    }

    private String trimStringNewline(String string) {
        while (string != null && string.endsWith("\n")) {
            string = string.substring(0, string.length() - 1);
        }

        return string;
    }

    @Override
    public void drawSplitString(@Nonnull String string, int x, int y, int width, int textColor) {
        this.resetStyles();
        this.textColor = textColor;
        string = this.trimStringNewline(string);
        this.renderSplitString(string, x, y, width);
    }

    private void renderSplitString(String string, int x, int y, int width) {
        for (Iterator<String> iterator = this.listFormattedStringToWidth(string, width).iterator(); iterator.hasNext(); y += this.FONT_HEIGHT) {
            String s = iterator.next();
            this.renderStringAligned(s, x, y, width, this.textColor);
        }
    }

    @Override
    public int getWordWrappedHeight(@Nonnull String string, int width) {
        return this.FONT_HEIGHT * this.listFormattedStringToWidth(string, width).size();
    }

    @Override
    public void setUnicodeFlag(boolean unicodeFlag) {
        this.unicodeFlag = unicodeFlag;
    }

    @Override
    public boolean getUnicodeFlag() {
        return this.unicodeFlag;
    }

    @Override
    public void setBidiFlag(boolean bidiFlag) {
        this.bidiFlag = bidiFlag;
    }

    @Override
    @Nonnull
    public List<String> listFormattedStringToWidth(@Nonnull String string, int width) {
        return Arrays.asList(this.wrapFormattedStringToWidth(string, width).split("\n"));
    }

    String wrapFormattedStringToWidth(String string, int width) {
        int i = this.sizeStringToWidth(string, width);
        if (string.length() <= i) {
            return string;
        } else {
            String s = string.substring(0, i);
            char c0 = string.charAt(i);
            boolean flag = c0 == ' ' || c0 == '\n';
            String s1 = getFormatFromString(s) + string.substring(i + (flag ? 1 : 0));
            return s + "\n" + this.wrapFormattedStringToWidth(s1, width);
        }
    }

    private int sizeStringToWidth(String string, int width) {
        int i = string.length();
        int j = 0;
        int k = 0;
        int l = -1;

        for (boolean flag = false; k < i; ++k) {
            char c0 = string.charAt(k);
            switch (c0) {
                case '\n':
                    --k;
                    break;
                case ' ':
                    l = k;
                default:
                    j += this.getCharWidth(c0);
                    if (flag) {
                        ++j;
                    }
                    break;
                case '\u00A7':
                    if (k < i - 1) {
                        ++k;
                        char c1 = string.charAt(k);
                        if (c1 != 'l' && c1 != 'L') {
                            if (c1 == 'r' || c1 == 'R' || isFormatColor(c1)) {
                                flag = false;
                            }
                        } else {
                            flag = true;
                        }
                    }
            }

            if (c0 == '\n') {
                ++k;
                l = k;
                break;
            }

            if (j > width) {
                break;
            }
        }

        return k != i && l != -1 && l < k ? l : k;
    }

    private static boolean isFormatColor(char ch) {
        return ch >= '0' && ch <= '9' || ch >= 'a' && ch <= 'f' || ch >= 'A' && ch <= 'F';
    }

    private static boolean isFormatSpecial(char ch) {
        return ch >= 'k' && ch <= 'o' || ch >= 'K' && ch <= 'O' || ch == 'r' || ch == 'R';
    }

    @Nonnull
    public static String getFormatFromString(@Nonnull String string) {
        int i = -1;
        int j = string.length();

        StringBuilder builder = new StringBuilder(16);
        while ((i = string.indexOf(167, i + 1)) != -1) {
            if (i < j - 1) {
                char c0 = string.charAt(i + 1);
                if (isFormatColor(c0)) {
                    builder.setLength(0);
                    builder.append("\u00A7").append(c0);
                } else if (isFormatSpecial(c0)) {
                    builder.append("\u00A7").append(c0);
                }
            }
        }
        return builder.toString();
    }

    @Override
    public boolean getBidiFlag() {
        return this.bidiFlag;
    }

    @Override
    protected void setColor(float r, float g, float b, float a) {
        GlStateManager.color(r, g, b, a);
    }

    @Override
    protected void enableAlpha() {
        GlStateManager.enableAlpha();
    }

    @Override
    @Nonnull
    protected IResource getResource(@Nonnull ResourceLocation location) throws IOException {
        return Minecraft.getMinecraft().getResourceManager().getResource(location);
    }

    @Override
    public int getColorCode(char ch) {
        int i = "0123456789abcdef".indexOf(ch);
        return i >= 0 && i < this.colorCode.length ? this.colorCode[i] : -1;
    }
}
