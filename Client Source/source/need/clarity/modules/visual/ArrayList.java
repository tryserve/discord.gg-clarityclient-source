package need.clarity.modules.visual;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.ModuleRegistry;
import need.clarity.nvg.ClarityDraw;
import need.clarity.nvg.HudElements;
import need.clarity.nvg.HudFx;
import need.clarity.nvg.HudLayout;
import need.clarity.util.ThemeColors;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NanoVG;

public class ArrayList extends ModuleBase {
   public static ArrayList INSTANCE;
   private static final float SUFFIX_SCALE = 0.8F;
   private static final NVGColor COL_SCRATCH = NVGColor.create();
   private static final NVGColor COL_SCRATCH2 = NVGColor.create();
   private static final Map<String, Float> animWidths = new HashMap<>();
   private static final Map<String, Float> measuredWidths = new HashMap<>();
   private static final Map<String, Float> animAlpha = new HashMap<>();
   private static float gradientHue = 0.0F;
   private static long lastFrameMs = 0L;
   private static String lastSig = "";
   private static final Map<String, String> modeStrs = new HashMap<>();
   private static final float MODE_GAP = 0.4F;

   public ArrayList() {
      super("ArrayList", "HUD");
      INSTANCE = this;
   }

   public static boolean wantsBlur() {
      ArrayList i = INSTANCE;
      return i != null && i.isEnabled() && i.getSetting("Blur", false);
   }

   public static int blurStrength() {
      ArrayList i = INSTANCE;
      return i == null ? 2 : Math.max(1, Math.min(4, i.getSetting("Blur Strength", 2)));
   }

   public static void draw(long vg, float w, float h) {
      ArrayList self = INSTANCE;
      if (self != null) {
         int accentRgb = ThemeColors.getAccentRgb();
         int accent2Rgb = ThemeColors.getAccent2Rgb();
         int surfaceRgb = mixRgb(ThemeColors.getSurfaceRgb(), accentRgb, 0.02F);
         float accentHue = rgbHue(accentRgb);
         String sideStr = self.getSetting("Side", "Right");
         boolean rightSide = !"Left".equalsIgnoreCase(sideStr);
         float scale = Math.max(0.1F, self.getSetting("Scale", 1.0F));
         int marginX = self.getSetting("Margin X", 6);
         int marginY = self.getSetting("Margin Y", 12);
         float rowH = self.getSetting("Row Height", 22.0F);
         float hPad = self.getSetting("H Padding", 18.0F);
         float textSize = self.getSetting("Text Size", 12.0F);
         float animSpeed = clamp(self.getSetting("Anim Speed", 0.12F), 0.01F, 1.0F);
         float cornerRadius = self.getSetting("Corner Radius", 4.0F);
         float rowGap = clamp(self.getSetting("Row Gap", 0.0F), 0.0F, 8.0F);
         boolean flushEdges = self.getSetting("Flush Edges", false);
         boolean border = self.getSetting("Border", true);
         int borderThemeRgb = mixRgb(ThemeColors.getBorderRgb(), accentRgb, 0.1F);
         int borderRgb = themedColor(self, "Border Color", borderThemeRgb);
         float borderW = clamp(self.getSetting("Border Width", 1.0F), 0.25F, 4.0F);
         boolean borderHl = self.getSetting("Border Highlight", true);
         int hlRgb = themedColor(self, "Highlight Color", ThemeColors.isLight() ? 0 : 16777215);
         float hlOpacity = clamp(self.getSetting("Highlight Opacity", 20).intValue(), 0.0F, 100.0F) / 100.0F;
         String bgMode = self.getSetting("Background", "Solid");
         int bgColorARGB = colorSetting(self, "BG Color", 0);
         float bgOpacity = clamp(self.getSetting("BG Opacity", 100).intValue(), 0.0F, 100.0F) / 100.0F;
         boolean useGradient = "Gradient".equalsIgnoreCase(bgMode);
         float gradSpeed = self.getSetting("Gradient Speed", 1.0F);
         float gradSat = clamp(self.getSetting("Gradient Saturation", 0.8F), 0.0F, 1.0F);
         int outlineARGB = themedColor(self, "Outline Color", borderThemeRgb);
         float outlineW = self.getSetting("Outline Width", 1.0F);
         int textARGB = themedColor(self, "Text Color", ThemeColors.getTextRgb());
         float textOpacity = clamp(self.getSetting("Text Opacity", 100).intValue(), 0.0F, 100.0F) / 100.0F;
         boolean shadow = self.getSetting("Shadow", false);
         int shadowARGB = themedColor(self, "Shadow Color", 0);
         float shadowOp = clamp(self.getSetting("Shadow Opacity", 60).intValue(), 0.0F, 100.0F) / 100.0F;
         boolean accentBar = self.getSetting("Accent Bar", false);
         int accentARGB = ThemeColors.resolveArgb(self.getSetting("Accent Color", null), 0xFF000000 | accentRgb, false);
         float accentW = self.getSetting("Accent Width", 2.0F);
         boolean showMode = self.getSetting("Show Mode", true);
         String modeStyle = self.getSetting("Mode Style", "Plain");
         int modeRgb = parseHexColor(self.getSetting("Mode Color", "#8A8F98"), 9080728) & 16777215;
         float modeOpacity = clamp(self.getSetting("Mode Opacity", 100).intValue(), 0.0F, 100.0F) / 100.0F;
         float modeScale = clamp(self.getSetting("Mode Scale", 1.0F), 0.5F, 1.5F);
         boolean showSuffix = self.getSetting("Show Suffix", false);
         float suffixOp = clamp(self.getSetting("Suffix Opacity", 55).intValue(), 0.0F, 100.0F) / 100.0F;
         long now = System.currentTimeMillis();
         if (useGradient && lastFrameMs > 0L) {
            float dtSec = Math.min((float)(now - lastFrameMs) / 1000.0F, 0.1F);
            gradientHue = (gradientHue + gradSpeed * 54.0F * dtSec) % 360.0F;
         }

         lastFrameMs = now;
         List<ModuleBase> all = ModuleRegistry.all();
         java.util.ArrayList<ModuleBase> active = new java.util.ArrayList<>(all.size());

         for (int i = 0; i < all.size(); i++) {
            ModuleBase m = all.get(i);
            if (m.isEnabled() && !(m instanceof ArrayList)) {
               active.add(m);
            }
         }

         if (active.isEmpty()) {
            decayAll(animWidths, animSpeed);
            if (HudLayout.isEditing()) {
               float fw = 90.0F * scale;
               HudLayout.report("ArrayList", rightSide ? w - marginX - fw : marginX, marginY, fw, rowH * scale);
            }
         } else {
            NanoVG.nvgFontFace(vg, "inter-sb");
            NanoVG.nvgFontSize(vg, textSize);
            NanoVG.nvgTextLetterSpacing(vg, 0.0F);
            NanoVG.nvgTextAlign(vg, 9);
            String sig = textSize + "|" + showSuffix + "|" + showMode + "|" + modeStyle + "|" + modeScale;
            if (!sig.equals(lastSig)) {
               measuredWidths.clear();
               lastSig = sig;
            }

            for (int i = 0; i < active.size(); i++) {
               ModuleBase m = active.get(i);
               String name = m.getName();
               String mode = showMode ? ArrayListModes.modeOf(name) : null;
               if (!measuredWidths.containsKey(name) || !Objects.equals(mode, modeStrs.get(name))) {
                  measuredWidths.put(name, rowTextWidth(vg, name, mode, textSize, showSuffix, modeStyle, modeScale));
                  if (mode == null) {
                     modeStrs.remove(name);
                  } else {
                     modeStrs.put(name, mode);
                  }
               }
            }

            active.sort((a, b) -> Float.compare(measuredWidths.getOrDefault(b.getName(), 0.0F), measuredWidths.getOrDefault(a.getName(), 0.0F)));

            for (int i = 0; i < active.size(); i++) {
               String name = active.get(i).getName();
               float target = measuredWidths.getOrDefault(name, 0.0F) + hPad;
               float cur = animWidths.getOrDefault(name, 0.0F);
               if (cur == 0.0F) {
                  cur = target;
               }

               animWidths.put(name, cur + (target - cur) * animSpeed);
               float ac = animAlpha.getOrDefault(name, 0.0F);
               animAlpha.put(name, ac + (1.0F - ac) * animSpeed);
            }

            for (String key : animWidths.keySet().toArray(new String[0])) {
               if (!measuredWidths.containsKey(key) || !isActive(active, key)) {
                  float cur = animWidths.get(key);
                  float next = cur * (1.0F - animSpeed);
                  float aa = animAlpha.getOrDefault(key, 1.0F) * (1.0F - animSpeed);
                  animAlpha.put(key, aa);
                  if (next < 0.5F && aa < 0.03F) {
                     animWidths.remove(key);
                     measuredWidths.remove(key);
                     animAlpha.remove(key);
                     modeStrs.remove(key);
                  } else {
                     animWidths.put(key, next);
                  }
               }
            }

            NanoVG.nvgSave(vg);
            NanoVG.nvgScale(vg, scale, scale);
            NanoVG.nvgTranslate(vg, 0.0F, marginY / scale);
            float localW = w / scale;
            float anchorX = rightSide ? localW - marginX / scale : marginX / scale;
            float y = 0.0F;
            int cardIdx = 0;
            boolean noBg = "None".equalsIgnoreCase(bgMode);
            boolean blurOn = !noBg && !"Outline Only".equalsIgnoreCase(bgMode) && self.getSetting("Blur", false) && HudElements.backdropReady();
            float blurMul = clamp(self.getSetting("Blur Tint", 0.5F), 0.0F, 1.0F);
            List<String> renderNames = new java.util.ArrayList<>();
            Map<String, ModuleBase> activeMap = new HashMap<>();

            for (ModuleBase mm : active) {
               renderNames.add(mm.getName());
               activeMap.put(mm.getName(), mm);
            }

            for (String k : animWidths.keySet()) {
               if (!activeMap.containsKey(k)) {
                  renderNames.add(k);
               }
            }

            float bbW = 0.0F;
            float bbBottom = 0.0F;

            for (int i = 0; i < renderNames.size(); i++) {
               String mName = renderNames.get(i);
               float animW = animWidths.getOrDefault(mName, 0.0F);
               if (animW < 1.0F) {
                  y += rowH + rowGap;
               } else {
                  bbW = Math.max(bbW, animW);
                  bbBottom = y + rowH;
                  float ani = clamp(animAlpha.getOrDefault(mName, 1.0F), 0.0F, 1.0F);
                  float cardX = rightSide ? anchorX - animW : anchorX;
                  cardX += (1.0F - ani) * (rightSide ? 12.0F : -12.0F);
                  float bl = cornerRadius;
                  float br = cornerRadius;
                  float tr = cornerRadius;
                  float tl = cornerRadius;
                  if (flushEdges) {
                     float eps = 0.5F;
                     float restX = rightSide ? anchorX - animW : anchorX;
                     float absY = y + marginY / scale;
                     boolean edgeL = restX <= 0.5F;
                     boolean edgeR = restX + animW >= localW - 0.5F;
                     boolean edgeT = absY <= 0.5F;
                     boolean edgeB = absY + rowH >= h / scale - 0.5F;
                     if (edgeT || edgeL) {
                        tl = 0.0F;
                     }

                     if (edgeT || edgeR) {
                        tr = 0.0F;
                     }

                     if (edgeB || edgeR) {
                        br = 0.0F;
                     }

                     if (edgeB || edgeL) {
                        bl = 0.0F;
                     }
                  }

                  boolean blurDrawn = blurOn && HudElements.fillBlurRRect(vg, cardX, y, animW, rowH, tl, tr, br, bl, 0.0F, -marginY / scale, w / scale, h / scale, ani);
                  float fillMul = blurDrawn ? blurMul : 1.0F;
                  if (!noBg) {
                     if ("Outline Only".equalsIgnoreCase(bgMode)) {
                        setNVGColorARGBAlpha(COL_SCRATCH, outlineARGB, 0.9F * ani);
                        NanoVG.nvgBeginPath(vg);
                        NanoVG.nvgRoundedRectVarying(vg, cardX, y, animW, rowH, tl, tr, br, bl);
                        NanoVG.nvgStrokeColor(vg, COL_SCRATCH);
                        NanoVG.nvgStrokeWidth(vg, outlineW);
                        NanoVG.nvgStroke(vg);
                     } else if (useGradient) {
                        float hue1 = (accentHue + gradientHue + cardIdx * 22.0F) % 360.0F;
                        float hue2 = (hue1 + 38.0F) % 360.0F;
                        setNVGColorHSLA(COL_SCRATCH, hue1, gradSat, 0.55F, bgOpacity * ani * fillMul);
                        setNVGColorHSLA(COL_SCRATCH2, hue2, gradSat, 0.48F, bgOpacity * ani * fillMul);
                        float gx0 = rightSide ? cardX + animW : cardX;
                        float gx1 = rightSide ? cardX : cardX + animW;
                        ClarityDraw.fillRRectVaryingLinear(vg, cardX, y, animW, rowH, tl, tr, br, bl, gx0, y, gx1, y, nvgColorToARGB(COL_SCRATCH), nvgColorToARGB(COL_SCRATCH2));
                     } else {
                        int a = Math.round(bgOpacity * ani * fillMul * 255.0F);
                        ClarityDraw.fillRRectVarying(vg, cardX, y, animW, rowH, tl, tr, br, bl, a << 24 | bgColorARGB & 16777215);
                     }
                  }

                  if (!noBg) {
                     HudFx.panel(vg, "ArrayList", cardX, y, animW, rowH, tl, tr, br, bl, ani);
                  }

                  if (border && !"Outline Only".equalsIgnoreCase(bgMode)) {
                     setNVGColorARGBAlpha(COL_SCRATCH, borderRgb, 1.0F * ani);
                     NanoVG.nvgBeginPath(vg);
                     NanoVG.nvgRoundedRectVarying(vg, cardX, y, animW, rowH, tl, tr, br, bl);
                     NanoVG.nvgStrokeColor(vg, COL_SCRATCH);
                     NanoVG.nvgStrokeWidth(vg, borderW);
                     NanoVG.nvgStroke(vg);
                     if (borderHl && hlOpacity > 0.0F) {
                        ClarityDraw.topHighlight(vg, cardX, y, animW, rowH, Math.max(tl, tr), Math.round(hlOpacity * ani * 255.0F) << 24 | hlRgb & 16777215);
                     }
                  }

                  if (accentBar && !noBg) {
                     float acX = rightSide ? cardX : cardX + animW - accentW;
                     setNVGColorARGBAlpha(COL_SCRATCH, accentARGB, 0.92F * ani);
                     NanoVG.nvgBeginPath(vg);
                     float ar = accentW / 2.0F;
                     float atl = rightSide ? (tl == 0.0F ? 0.0F : ar) : ar;
                     float abl = rightSide ? (bl == 0.0F ? 0.0F : ar) : ar;
                     float atr = rightSide ? ar : (tr == 0.0F ? 0.0F : ar);
                     float abr = rightSide ? ar : (br == 0.0F ? 0.0F : ar);
                     NanoVG.nvgRoundedRectVarying(vg, acX, y, accentW, rowH, atl, atr, abr, abl);
                     NanoVG.nvgFillColor(vg, COL_SCRATCH);
                     NanoVG.nvgFill(vg);
                  }

                  float cy = Math.round(y + rowH * 0.5F);
                  int fillArgb = Math.round(textOpacity * ani * 255.0F) << 24 | textARGB & 16777215;
                  float shadowA = shadowOp * textOpacity * ani;
                  String mode = showMode ? modeStrs.get(mName) : null;
                  if (!showSuffix && mode == null) {
                     float cx = Math.round(cardX + animW * 0.5F);
                     drawRowText(vg, textSize, fillArgb, 18, cx, cy, mName, shadow, shadowARGB, shadowA);
                  } else {
                     String sfx = showSuffix ? suffixOf(mName) : null;
                     String modeTx = mode == null ? null : modeText(mode, modeStyle);
                     float sfxSz = textSize * 0.8F;
                     float modeSz = textSize * modeScale;
                     float nameW = ClarityDraw.textWidth(vg, "inter-sb", textSize, 0.0F, mName);
                     float sfxW = sfx == null ? 0.0F : ClarityDraw.textWidth(vg, "inter-sb", sfxSz, 0.0F, sfx);
                     float modeW = modeTx == null ? 0.0F : ClarityDraw.textWidth(vg, "inter-sb", modeSz, 0.0F, modeTx);
                     float gap = modeTx == null ? 0.0F : textSize * 0.4F;
                     float total = nameW + sfxW + gap + modeW;
                     float sx = Math.round(cardX + animW * 0.5F - total * 0.5F);
                     int left = 17;
                     drawRowText(vg, textSize, fillArgb, left, sx, cy, mName, shadow, shadowARGB, shadowA);
                     sx += nameW;
                     if (sfx != null) {
                        int sfxArgb = Math.round(textOpacity * suffixOp * ani * 255.0F) << 24 | textARGB & 16777215;
                        drawRowText(vg, sfxSz, sfxArgb, left, sx, cy, sfx, shadow, shadowARGB, shadowA * suffixOp);
                        sx += sfxW;
                     }

                     if (modeTx != null) {
                        int modeArgb = Math.round(modeOpacity * ani * 255.0F) << 24 | modeRgb;
                        drawRowText(vg, modeSz, modeArgb, left, sx + gap, cy, modeTx, shadow, shadowARGB, shadowOp * modeOpacity * ani);
                     }
                  }

                  y += rowH + rowGap;
                  cardIdx++;
               }
            }

            if (bbW > 0.0F && HudLayout.isEditing()) {
               float localX = rightSide ? anchorX - bbW : anchorX;
               HudLayout.report("ArrayList", localX * scale, marginY, bbW * scale, bbBottom * scale);
            }

            NanoVG.nvgRestore(vg);
         }
      }
   }

   private static String modeText(String mode, String style) {
      if ("Brackets".equalsIgnoreCase(style)) {
         return "[" + mode + "]";
      } else {
         return "Dash".equalsIgnoreCase(style) ? "- " + mode : mode;
      }
   }

   private static float rowTextWidth(long vg, String name, String mode, float textSize, boolean showSuffix, String modeStyle, float modeScale) {
      float w = ClarityDraw.textWidth(vg, "inter-sb", textSize, 0.0F, name);
      if (showSuffix) {
         w += ClarityDraw.textWidth(vg, "inter-sb", textSize * 0.8F, 0.0F, suffixOf(name));
      }

      if (mode != null) {
         w += textSize * 0.4F + ClarityDraw.textWidth(vg, "inter-sb", textSize * modeScale, 0.0F, modeText(mode, modeStyle));
      }

      return w;
   }

   private static String suffixOf(String name) {
      return " [" + name.substring(0, Math.min(2, name.length())).toUpperCase() + "]";
   }

   private static void drawRowText(long vg, float size, int argb, int align, float x, float y, String s, boolean shadow, int shadowRgb, float shadowAlpha) {
      if (shadow && shadowAlpha > 0.01F) {
         int sa = Math.round(clamp(shadowAlpha, 0.0F, 1.0F) * 255.0F);
         ClarityDraw.hudText(vg, size, sa << 24 | shadowRgb & 16777215, align, x + 1.0F, y + 1.0F, s);
      }

      ClarityDraw.hudText(vg, size, argb, align, x, y, s);
   }

   private static int themedColor(ArrayList self, String key, int fallbackRgb) {
      Object v = self.getSetting(key, null);
      if (ThemeColors.isThemeValue(v)) {
         boolean second = v.toString().trim().equalsIgnoreCase("theme2");
         return (second ? ThemeColors.getAccent2Rgb() : ThemeColors.getAccentRgb()) & 16777215;
      } else {
         return ThemeColors.isEnabled() ? ThemeColors.getAccentRgb() & 16777215 : parseHexColor(v == null ? null : v.toString(), fallbackRgb) & 16777215;
      }
   }

   private static int colorSetting(ArrayList self, String key, int fallbackRgb) {
      Object v = self.getSetting(key, null);
      if (ThemeColors.isThemeValue(v)) {
         boolean second = v.toString().trim().equalsIgnoreCase("theme2");
         return (second ? ThemeColors.getAccent2Rgb() : ThemeColors.getAccentRgb()) & 16777215;
      } else {
         return parseHexColor(v == null ? null : v.toString(), fallbackRgb) & 16777215;
      }
   }

   public static boolean areAnimationsSettled() {
      for (Entry<String, Float> e : animAlpha.entrySet()) {
         float a = e.getValue();
         if (a < 0.985F && a > 0.015F) {
            return false;
         }
      }

      List<ModuleBase> all = ModuleRegistry.all();

      for (int i = 0; i < all.size(); i++) {
         ModuleBase m = all.get(i);
         if (!(m instanceof ArrayList) && m.isEnabled()) {
            float target = measuredWidths.getOrDefault(m.getName(), 0.0F);
            if (target != 0.0F) {
               float current = animWidths.getOrDefault(m.getName(), 0.0F);
               if (Math.abs(target - current) > 0.05F) {
                  return false;
               }
            }
         }
      }

      for (Entry<String, Float> e : animWidths.entrySet()) {
         if (!measuredWidths.containsKey(e.getKey()) && e.getValue() > 0.5F) {
            return false;
         }
      }

      return true;
   }

   private static boolean isActive(java.util.ArrayList<ModuleBase> active, String name) {
      for (int i = 0; i < active.size(); i++) {
         if (active.get(i).getName().equals(name)) {
            return true;
         }
      }

      return false;
   }

   private static void decayAll(Map<String, Float> map, float speed) {
      for (String k : map.keySet().toArray(new String[0])) {
         float next = map.get(k) * (1.0F - speed);
         float aa = animAlpha.getOrDefault(k, 1.0F) * (1.0F - speed);
         animAlpha.put(k, aa);
         if (next < 0.5F && aa < 0.03F) {
            map.remove(k);
            measuredWidths.remove(k);
            animAlpha.remove(k);
            modeStrs.remove(k);
         } else {
            map.put(k, next);
         }
      }
   }

   private static float clamp(float v, float lo, float hi) {
      return v < lo ? lo : Math.min(v, hi);
   }

   private static int mixRgb(int a, int b, float t) {
      t = clamp(t, 0.0F, 1.0F);
      int ar = a >> 16 & 0xFF;
      int ag = a >> 8 & 0xFF;
      int ab = a & 0xFF;
      int br = b >> 16 & 0xFF;
      int bg = b >> 8 & 0xFF;
      int bb = b & 0xFF;
      return Math.round(ar + (br - ar) * t) << 16 | Math.round(ag + (bg - ag) * t) << 8 | Math.round(ab + (bb - ab) * t);
   }

   private static float rgbHue(int rgb) {
      float r = (rgb >> 16 & 0xFF) / 255.0F;
      float g = (rgb >> 8 & 0xFF) / 255.0F;
      float b = (rgb & 0xFF) / 255.0F;
      float max = Math.max(r, Math.max(g, b));
      float min = Math.min(r, Math.min(g, b));
      float d = max - min;
      if (d <= 1.0E-4F) {
         return 0.0F;
      }

      float h;
      if (max == r) {
         h = (g - b) / d % 6.0F;
      } else if (max == g) {
         h = (b - r) / d + 2.0F;
      } else {
         h = (r - g) / d + 4.0F;
      }

      float deg = h * 60.0F;
      return deg < 0.0F ? deg + 360.0F : deg;
   }

   private static int parseHexColor(String hex, int fallback) {
      if (hex != null && !hex.isEmpty()) {
         String s = hex.startsWith("#") ? hex.substring(1) : hex;

         try {
            return (int)Long.parseLong(s, 16);
         } catch (NumberFormatException e) {
            return fallback;
         }
      } else {
         return fallback;
      }
   }

   private static void setNVGColorARGBAlpha(NVGColor c, int rgb, float alpha) {
      float r = (rgb >> 16 & 0xFF) / 255.0F;
      float g = (rgb >> 8 & 0xFF) / 255.0F;
      float b = (rgb & 0xFF) / 255.0F;
      NanoVG.nvgRGBAf(r, g, b, alpha, c);
   }

   private static void setNVGColorHSLA(NVGColor c, float h, float s, float l, float a) {
      float r;
      float g;
      float b;
      if (s == 0.0F) {
         b = l;
         g = l;
         r = l;
      } else {
         float q = l < 0.5F ? l * (1.0F + s) : l + s - l * s;
         float p = 2.0F * l - q;
         r = hue2rgb(p, q, h / 360.0F + 0.33333334F);
         g = hue2rgb(p, q, h / 360.0F);
         b = hue2rgb(p, q, h / 360.0F - 0.33333334F);
      }

      NanoVG.nvgRGBAf(r, g, b, a, c);
   }

   private static float hue2rgb(float p, float q, float t) {
      if (t < 0.0F) {
         t++;
      }

      if (t > 1.0F) {
         t--;
      }

      if (t < 0.16666667F) {
         return p + (q - p) * 6.0F * t;
      } else if (t < 0.5F) {
         return q;
      } else {
         return t < 0.6666667F ? p + (q - p) * (0.6666667F - t) * 6.0F : p;
      }
   }

   private static int nvgColorToARGB(NVGColor c) {
      int r = (int)(c.r() * 255.0F) & 0xFF;
      int g = (int)(c.g() * 255.0F) & 0xFF;
      int b = (int)(c.b() * 255.0F) & 0xFF;
      int a = (int)(c.a() * 255.0F) & 0xFF;
      return a << 24 | r << 16 | g << 8 | b;
   }
}
