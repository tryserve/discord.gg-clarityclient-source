package need.clarity.nvg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import need.clarity.clickgui.NvgCanvas;
import need.clarity.friends.Friend;
import need.clarity.friends.FriendManager;
import need.clarity.friends.Friends;
import need.clarity.modules.visual.HealthTags;
import need.clarity.modules.visual.Nametags;
import need.clarity.util.ClarityRenderUtils;
import need.clarity.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class NametagRenderer {
   private static final Map<Integer, NametagRenderer.Tag> TAGS = new HashMap<>();
   private static long lastNanos;
   private static final EquipmentSlot[] ARMOR = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

   private NametagRenderer() {
   }

   public static void draw(long vg, NvgCanvas canvas, Minecraft mc, float W, float H) {
      Nametags n = Nametags.INSTANCE;
      if (n != null && n.isEnabled() && mc.level != null && mc.player != null) {
         long now = System.nanoTime();
         float dt = lastNanos == 0L ? 0.016666668F : Math.min(0.1F, (float)(now - lastNanos) / 1.0E9F);
         lastNanos = now;
         float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
         Vec3 cam = ClarityRenderUtils.getCameraPos();
         float maxDist = n.maxDistance();
         List<NametagRenderer.Pending> list = new ArrayList<>();

         for (Entity e : mc.level.entitiesForRendering()) {
            if (n.shows(e)) {
               double d = Math.sqrt(e.distanceToSqr(cam));
               if (!(d > maxDist + 8.0F)) {
                  list.add(new NametagRenderer.Pending(e, d));
               }
            }
         }

         list.sort((a, b) -> Double.compare(b.dist(), a.dist()));
         String[] faces = McText.faces(n.font());
         float rate = 30.0F - 24.0F * n.smoothness();
         float k = 1.0F - (float)Math.exp(-rate * dt);

         for (NametagRenderer.Pending p : list) {
            Entity e = p.e();
            LivingEntity le = (LivingEntity)e;
            NametagRenderer.Tag t = TAGS.computeIfAbsent(e.getId(), id -> {
               NametagRenderer.Tag nt = new NametagRenderer.Tag();
               nt.hp = nt.trail = le.getHealth();
               return nt;
            });
            boolean inRange = p.dist() <= maxDist;
            boolean visible = n.throughWalls() || canSee(mc, cam, e);
            float target = inRange && visible ? 1.0F : 0.0F;
            if (n.fadeDistance() && inRange) {
               target *= Mth.clamp((maxDist - (float)p.dist()) / 8.0F, 0.0F, 1.0F);
            }

            t.alpha = t.alpha + (target - t.alpha) * (1.0F - (float)Math.exp(-14.0F * dt));
            t.seen = now;
            float hp = le.getHealth();
            t.hp = t.hp + (hp - t.hp) * k;
            if (hp >= t.trail) {
               t.trail = hp;
               t.trailHold = 0.0F;
            } else if (t.trailHold < 0.35F) {
               t.trailHold += dt;
            } else {
               t.trail = t.trail + (hp - t.trail) * (1.0F - (float)Math.exp(-5.0F * dt));
            }

            if (!(t.alpha < 0.01F)) {
               double x = Mth.lerp(pt, e.xOld, e.getX());
               double y = Mth.lerp(pt, e.yOld, e.getY()) + e.getBbHeight() + n.yOffset() + (e.isCrouching() ? -0.2 : 0.0);
               double z = Mth.lerp(pt, e.zOld, e.getZ());

               Vec3 ndc;
               try {
                  ndc = mc.gameRenderer.projectPointToScreen(new Vec3(x, y, z));
               } catch (Throwable ex) {
                  continue;
               }

               if (ndc != null && !(ndc.z <= 0.0) && !(ndc.z >= 1.0) && Double.isFinite(ndc.x) && Double.isFinite(ndc.y)) {
                  float sx = (float)((ndc.x * 0.5 + 0.5) * W);
                  float sy = (float)((1.0 - (ndc.y * 0.5 + 0.5)) * H);
                  if (!(sx < -200.0F) && !(sx > W + 200.0F) && !(sy < -200.0F) && !(sy > H + 200.0F)) {
                     float S = n.scale();
                     if ("Distance".equals(n.sizeMode())) {
                        S *= Mth.clamp(10.0F / (float)Math.max(1.0, p.dist()), n.minScale(), 1.25F);
                     }

                     drawTag(vg, canvas, mc, n, faces, le, t, sx, sy, S, (float)p.dist());
                  }
               }
            }
         }

         Iterator<Entry<Integer, NametagRenderer.Tag>> it = TAGS.entrySet().iterator();

         while (it.hasNext()) {
            if (now - it.next().getValue().seen > 1000000000L) {
               it.remove();
            }
         }
      } else {
         TAGS.clear();
      }
   }

   private static boolean canSee(Minecraft mc, Vec3 cam, Entity e) {
      try {
         Vec3 to = new Vec3(e.getX(), e.getEyeY(), e.getZ());
         HitResult r = mc.level.clip(new ClipContext(cam, to, Block.COLLIDER, Fluid.NONE, mc.player));
         return r == null || r.getType() == Type.MISS;
      } catch (Throwable t) {
         return true;
      }
   }

   private static void drawTag(long vg, NvgCanvas canvas, Minecraft mc, Nametags n, String[] faces, LivingEntity le, NametagRenderer.Tag t, float cx, float bottom, float S, float dist) {
      float a = t.alpha;
      String style = n.style();
      boolean minimal = "Minimal".equals(style);
      boolean classic = "Classic".equals(style);
      boolean glass = "Glass".equals(style);
      int accent = ThemeColors.getAccentRgb();
      int textRgb = n.themeColors() ? ThemeColors.getTextRgb() : ThemeColors.resolveRgb(n.textColor(), 15592944, false) & 16777215;
      int bgRgb = n.themeColors() ? ThemeColors.getSurfaceRgb() : ThemeColors.resolveRgb(n.bgColor(), 1119260, false) & 16777215;
      int mutedRgb = mix(textRgb, bgRgb, 0.42F);
      if (classic) {
         textRgb = 16777215;
         bgRgb = 0;
         mutedRgb = 11184810;
      }

      float fs = 9.0F * S;
      float fsSmall = 7.0F * S;
      float padX = (minimal ? 2.0F : 6.0F) * S;
      float padY = (minimal ? 1.0F : 3.5F) * S;
      List<McText.Run> name = nameRuns(n, le, textRgb);
      Friend friend = le instanceof Player ? Friends.of(le) : null;
      String star = friend != null && n.friendMark() ? "★ " : "";
      int friendRgb = friend != null ? friendColour(friend, accent) : accent;
      String hm = n.healthMode();
      boolean barOn = hm.contains("Bar");
      boolean numOn = hm.contains("Number") || "Hearts".equals(hm);
      float hpMax = Math.max(1.0F, le.getMaxHealth());
      float abs = n.absorption() ? le.getAbsorptionAmount() : 0.0F;
      String hpText = numOn ? hpText(n, t.hp + abs, hpMax, hm) : "";
      int hpRgb = healthRgb(n, t.hp / hpMax, accent);
      String extra = "";
      if (le instanceof Player pl) {
         PlayerInfo info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(pl.getUUID());
         if (n.gamemode() && info != null && info.getGameMode() != null) {
            extra = extra + gm(info.getGameMode().name()) + " ";
         }

         if (n.ping() && info != null && !mc.isLocalServer()) {
            extra = extra + info.getLatency() + "ms ";
         }
      }

      if (n.distance() && le != mc.player) {
         extra = extra + String.format(Locale.ROOT, dist < 10.0F ? "%.1fm" : "%.0fm", dist);
      }

      extra = extra.trim();
      float starW = star.isEmpty() ? 0.0F : ClarityDraw.textWidth(vg, faces[2], fs, 0.0F, star);
      float nameW = McText.width(vg, faces, fs, name);
      float hpW = hpText.isEmpty() ? 0.0F : ClarityDraw.textWidth(vg, faces[2], fs, 0.0F, hpText) + 4.0F * S;
      float extraW = extra.isEmpty() ? 0.0F : ClarityDraw.textWidth(vg, faces[1], fsSmall, 0.0F, extra) + 5.0F * S;
      float lineW = starW + nameW + hpW + extraW;
      float barH = barOn ? (minimal ? 1.6F : 2.6F) * S : 0.0F;
      float barGap = barOn ? 2.6F * S : 0.0F;
      float w = Math.max(lineW, barOn ? 46.0F * S : 0.0F) + padX * 2.0F;
      float h = padY * 2.0F + fs * 1.05F + barGap + barH;
      float x = cx - w / 2.0F;
      float y = bottom - h;
      if (!minimal) {
         float r = classic ? 0.0F : n.radius() * S;
         float bgA = n.bgOpacity() * (classic ? 0.35F : 1.0F) * a;
         if (!classic) {
            ClarityDraw.fillRRect(vg, x, y + 1.2F * S, w, h, r, argb(0, 0.25F * a));
         }

         ClarityDraw.fillRRect(vg, x, y, w, h, r, argb(glass ? mix(bgRgb, 16777215, 0.08F) : bgRgb, glass ? bgA * 0.55F : bgA));
         HudFx.panel(vg, "Nametags", x, y, w, h, r, a);
         if (n.border() && !classic) {
            ClarityDraw.strokeRRect(vg, x, y, w, h, r, Math.max(0.6F, 0.8F * S), argb(glass ? 16777215 : mix(ThemeColors.getBorderRgb(), accent, 0.15F), (glass ? 0.22F : 0.85F) * a));
         }

         if (n.accentLine() && !classic && !glass) {
            float lw = Math.min(w - r * 2.0F, w * 0.55F);
            ClarityDraw.fillRRect(vg, cx - lw / 2.0F, y, lw, Math.max(1.0F, 1.2F * S), 1.0F, argb(friend != null ? friendRgb : accent, 0.9F * a));
         }
      }

      float ly = y + padY + fs * 0.55F;
      float tx = x + padX;
      boolean shadow = n.textShadow() || minimal || classic;
      if (!star.isEmpty()) {
         ClarityDraw.text(vg, faces[2], fs, 0.0F, argb(friendRgb, a), 17, tx, ly, star);
         tx += starW;
      }

      tx = McText.draw(vg, faces, fs, name, tx, ly, a, shadow, null);
      if (!hpText.isEmpty()) {
         tx += 4.0F * S;
         ClarityDraw.text(vg, faces[2], fs, 0.0F, argb(hpRgb, a), 17, tx, ly, hpText);
      }

      if (!extra.isEmpty()) {
         ClarityDraw.text(vg, faces[1], fsSmall, 0.0F, argb(mutedRgb, a), 20, x + w - padX, ly + 0.4F * S, extra);
      }

      if (barOn) {
         float bx = x + padX;
         float bw = w - padX * 2.0F;
         float by = ly + fs * 0.5F + barGap - barH * 0.2F;
         float br = barH / 2.0F;
         ClarityDraw.fillRRect(vg, bx, by, bw, barH, br, argb(mix(bgRgb, textRgb, 0.14F), (minimal ? 0.6F : 0.9F) * a));
         float total = hpMax + abs;
         float fTrail = Mth.clamp(t.trail / total, 0.0F, 1.0F);
         float fHp = Mth.clamp(t.hp / total, 0.0F, 1.0F);
         float fAbs = Mth.clamp(abs / total, 0.0F, 1.0F);
         if (n.damageTrail() && fTrail > fHp + 0.002F) {
            ClarityDraw.fillRRect(vg, bx, by, bw * fTrail, barH, br, argb(16777215, 0.55F * a));
         }

         if (fHp > 0.002F) {
            ClarityDraw.fillRRect(vg, bx, by, Math.max(barH, bw * fHp), barH, br, argb(hpRgb, a));
         }

         if (fAbs > 0.002F) {
            ClarityDraw.fillRRect(vg, bx + bw * fHp, by, Math.max(barH, bw * fAbs), barH, br, argb(16106818, a));
         }
      }

      if ((n.armor() || n.heldItems()) && a > 0.9F) {
         List<ItemStack> items = new ArrayList<>();
         if (n.heldItems()) {
            items.add(le.getMainHandItem());
         }

         if (n.armor()) {
            for (EquipmentSlot s : ARMOR) {
               items.add(le.getItemBySlot(s));
            }
         }

         if (n.heldItems()) {
            items.add(le.getOffhandItem());
         }

         items.removeIf(ItemStack::isEmpty);
         if (!items.isEmpty()) {
            String durMode = n.durability();
            boolean barMode = durMode.contains("Bar");
            boolean pctMode = durMode.contains("Percent");
            boolean numMode = durMode.contains("Number");
            boolean textMode = pctMode || numMode;
            float is = 12.0F * S;
            float pfs = Math.max(4.5F, fsSmall * 0.74F);
            float gap = (!textMode && !n.enchants() ? 1.5F : 4.0F) * S;
            boolean anyDamageable = false;
            List<List<String[]>> ench = new ArrayList<>();
            int maxLines = 0;

            for (ItemStack st : items) {
               if (st.isDamageableItem() && st.getMaxDamage() > 0) {
                  anyDamageable = true;
               }

               List<String[]> e = n.enchants() ? enchantLines(st, n.maxEnchants()) : List.of();
               ench.add(e);
               maxLines = Math.max(maxLines, e.size());
            }

            float under = !anyDamageable ? 0.0F : (barMode ? 2.4F * S : 0.0F) + (textMode ? pfs * 1.15F : 0.0F);
            float lineH = pfs * 1.05F;
            float rowW = items.size() * is + (items.size() - 1) * gap;
            float ix = cx - rowW / 2.0F;
            float iy = y - is - under - 2.0F * S;

            for (int k = 0; k < items.size(); k++) {
               ItemStack st = items.get(k);
               canvas.recordStack(st, null, ix, iy, is, 0);
               if (st.isDamageableItem() && st.getMaxDamage() > 0) {
                  int max = st.getMaxDamage();
                  int leftPts = Math.max(0, max - st.getDamageValue());
                  float left = Mth.clamp((float)leftPts / max, 0.0F, 1.0F);
                  int col = healthRgb(n, left, accent);
                  float uy = iy + is + 0.6F * S;
                  if (barMode) {
                     float bw = is * 0.78F;
                     float bx = ix + (is - bw) / 2.0F;
                     float bh = Math.max(1.0F, 1.2F * S);
                     ClarityDraw.fillRRect(vg, bx, uy, bw, bh, bh / 2.0F, argb(0, 0.55F * a));
                     ClarityDraw.fillRRect(vg, bx, uy, Math.max(bh, bw * left), bh, bh / 2.0F, argb(col, a));
                     uy += 2.4F * S - 0.6F * S;
                  }

                  if (textMode) {
                     String label = numMode ? String.valueOf(leftPts) : Math.round(left * 100.0F) + "%";
                     if (shadow) {
                        ClarityDraw.text(vg, faces[2], pfs, 0.0F, argb(0, 0.6F * a), 10, ix + is / 2.0F + 0.5F * S, uy + 0.5F * S, label);
                     }

                     ClarityDraw.text(vg, faces[2], pfs, 0.0F, argb(col, a), 10, ix + is / 2.0F, uy, label);
                  }
               }

               List<String[]> e = ench.get(k);

               for (int li = 0; li < e.size(); li++) {
                  String[] en = e.get(li);
                  float ty = iy - 0.8F * S - li * lineH;
                  int col = en[1] != null ? 16733525 : mix(13215999, textRgb, 0.25F);
                  if (shadow) {
                     ClarityDraw.text(vg, faces[2], pfs, 0.0F, argb(0, 0.6F * a), 34, ix + is / 2.0F + 0.5F * S, ty + 0.5F * S, en[0]);
                  }

                  ClarityDraw.text(vg, faces[2], pfs, 0.0F, argb(col, a), 34, ix + is / 2.0F, ty, en[0]);
               }

               ix += is + gap;
            }
         }
      }
   }

   private static List<String[]> enchantLines(ItemStack st, int max) {
      List<String[]> out = new ArrayList<>();

      try {
         ItemEnchantments ie = (ItemEnchantments)st.get(DataComponents.ENCHANTMENTS);
         if (ie == null || ie.isEmpty()) {
            ie = (ItemEnchantments)st.get(DataComponents.STORED_ENCHANTMENTS);
         }

         if (ie == null || ie.isEmpty()) {
            return out;
         }

         for (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<Holder<Enchantment>> entry : ie.entrySet()) {
            if (out.size() >= max) {
               break;
            }

            Holder<Enchantment> h = (Holder<Enchantment>)entry.getKey();
            int lvl = entry.getIntValue();
            String full = ((Enchantment)h.value()).description().getString();
            boolean curse = full.toLowerCase(Locale.ROOT).startsWith("curse");
            int maxLvl = 1;

            try {
               maxLvl = ((Enchantment)h.value()).getMaxLevel();
            } catch (Throwable var12) {
            }

            out.add(new String[]{abbreviate(full) + (maxLvl <= 1 && lvl <= 1 ? "" : String.valueOf(lvl)), curse ? "c" : null});
         }
      } catch (Throwable var13) {
      }

      return out;
   }

   private static String abbreviate(String name) {
      String n = name.replaceFirst("(?i)^curse of ", "").trim();
      String[] w = n.split("\\s+");
      if (w.length >= 2) {
         String a = w[0].length() >= 2 ? w[0].substring(0, 2) : w[0];
         return a + Character.toUpperCase(w[w.length - 1].charAt(0));
      } else {
         return n.length() <= 3 ? n : n.substring(0, 3);
      }
   }

   private static List<McText.Run> nameRuns(Nametags n, LivingEntity le, int textRgb) {
      String mode = n.nameColor();
      if ("Team".equals(mode)) {
         try {
            Component c = le.getDisplayName();
            List<McText.Run> runs = McText.runs(c, textRgb);
            if (!runs.isEmpty()) {
               return runs;
            }
         } catch (Throwable var7) {
         }
      }
      int rgb = switch (mode) {
         case "Theme" -> ThemeColors.getAccentRgb();
         case "White" -> 16777215;
         default -> textRgb;
      };
      return McText.runs(Component.literal(le.getName().getString()), rgb);
   }

   private static String hpText(Nametags n, float hp, float max, String mode) {
      if ("Hearts".equals(mode)) {
         return String.format(Locale.ROOT, "❤ %.1f", hp / 2.0F);
      }

      return switch (n.healthFormat()) {
         case "Hearts" -> String.format(Locale.ROOT, "%.1f", hp / 2.0F);
         case "Percent" -> Math.round(100.0F * hp / max) + "%";
         default -> String.format(Locale.ROOT, !(hp >= 10.0F) && hp != Math.floor(hp) ? "%.1f" : "%.0f", hp);
      };
   }

   private static int healthRgb(Nametags n, float f, int accent) {
      f = Mth.clamp(f, 0.0F, 1.0F);

      return switch (n.barColor()) {
         case "Theme" -> accent;
         case "Classic" -> HealthTags.getHealthColor(f, 1.0F) & 16777215;
         default -> hsv(f * 0.33F, 0.62F, 0.98F);
      };
   }

   private static int hsv(float h, float s, float v) {
      float h6 = (h - (float)Math.floor(h)) * 6.0F;
      int i = (int)h6;
      float f = h6 - i;
      float p = v * (1.0F - s);
      float q = v * (1.0F - s * f);
      float u = v * (1.0F - s * (1.0F - f));
      float r;
      float g;
      float b;
      switch (i) {
         case 0:
            r = v;
            g = u;
            b = p;
            break;
         case 1:
            r = q;
            g = v;
            b = p;
            break;
         case 2:
            r = p;
            g = v;
            b = u;
            break;
         case 3:
            r = p;
            g = q;
            b = v;
            break;
         case 4:
            r = u;
            g = p;
            b = v;
            break;
         default:
            r = v;
            g = p;
            b = q;
      }

      return Math.round(r * 255.0F) << 16 | Math.round(g * 255.0F) << 8 | Math.round(b * 255.0F);
   }

   private static int friendColour(Friend f, int fallback) {
      try {
         int c = FriendManager.nameColour(f);
         return c >= 0 ? c & 16777215 : 8188072;
      } catch (Throwable t) {
         return fallback;
      }
   }

   private static String gm(String name) {
      return switch (name) {
         case "CREATIVE" -> "[C]";
         case "SPECTATOR" -> "[SP]";
         case "ADVENTURE" -> "[A]";
         default -> "[S]";
      };
   }

   private static int argb(int rgb, float a) {
      int al = Math.round(Mth.clamp(a, 0.0F, 1.0F) * 255.0F);
      return al << 24 | rgb & 16777215;
   }

   private static int mix(int x, int y, float t) {
      int xr = x >> 16 & 0xFF;
      int xg = x >> 8 & 0xFF;
      int xb = x & 0xFF;
      int yr = y >> 16 & 0xFF;
      int yg = y >> 8 & 0xFF;
      int yb = y & 0xFF;
      return Math.round(xr + (yr - xr) * t) << 16 | Math.round(xg + (yg - xg) * t) << 8 | Math.round(xb + (yb - xb) * t);
   }

   private static final class Pending {
      final Entity e;
      final double dist;

      Pending(Entity e, double dist) {
         this.e = e;
         this.dist = dist;
      }

      Entity e() {
         return this.e;
      }

      double dist() {
         return this.dist;
      }
   }

   private static final class Tag {
      float alpha;
      float hp;
      float trail;
      float trailHold;
      long seen;
   }
}
