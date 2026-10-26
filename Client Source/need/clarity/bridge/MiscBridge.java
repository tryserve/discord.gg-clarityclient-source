package need.clarity.bridge;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import need.clarity.modules.ModuleBase;
import need.clarity.modules.ModuleRegistry;
import need.clarity.modules.movement.AntiPowderSnow;
import need.clarity.modules.utility.ClientSpoofer;
import need.clarity.modules.utility.NameProtect;
import need.clarity.modules.utility.Overtick;
import need.clarity.modules.visual.CrystalView;
import need.clarity.modules.visual.Freecam;
import need.clarity.modules.visual.MoveAnimations;
import need.clarity.modules.visual.NoRender;
import need.clarity.modules.visual.PlayerModel;
import need.clarity.modules.visual.SkinChanger;
import need.clarity.render.ModelHands;
import need.clarity.render.esp.ShaderEspRenderer;
import need.clarity.render.sky.SkyShaderRenderer;
import need.clarity.util.ThemeTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class MiscBridge {
   private static volatile int resLogCount = 0;
   private static volatile boolean loggedNs = false;
   private static volatile Object wrappedRm = null;
   private static volatile Object lastRealRm = null;
   private static volatile boolean loggedBuildFail = false;

   private MiscBridge() {
   }

   public static String getClientBrand(String original) {
      try {
         ClientSpoofer module = ClientSpoofer.getInstance();
         if (module != null && module.isEnabled()) {
            return module.getBrand();
         }
      } catch (Throwable var2) {
      }

      return original;
   }

   public static boolean canWalkOnPowderSnow(Object entity, boolean original) {
      try {
         AntiPowderSnow m = AntiPowderSnow.INSTANCE;
         if (m == null || !m.isEnabled()) {
            return original;
         } else {
            return entity != Minecraft.getInstance().player ? original : true;
         }
      } catch (Throwable t) {
         return original;
      }
   }

   public static boolean shouldBypassOcclusionCulling() {
      try {
         return Freecam.isActive();
      } catch (Throwable t) {
         return false;
      }
   }

   public static Object nameProtectChat(Object messageObj) {
      try {
         NameProtect np = NameProtect.getInstance();
         return np != null && np.isEnabled() ? NameProtect.replaceInTree((Component)messageObj) : messageObj;
      } catch (Throwable t) {
         return messageObj;
      }
   }

   public static String nameProtectChatInput(String input) {
      return input;
   }

   public static Object nameProtectTabList(Object playerInfoObj, Object componentObj) {
      try {
         NameProtect m = NameProtect.getInstance();
         if (m == null || !m.isEnabled()) {
            return componentObj;
         }

         Minecraft mc = Minecraft.getInstance();
         if (mc.player == null) {
            return componentObj;
         }

         PlayerInfo pi = (PlayerInfo)playerInfoObj;
         String profileName = pi.getProfile().name();
         if (!profileName.equals(mc.player.getGameProfile().name())) {
            return componentObj;
         }

         String spoof = m.getSpoofName();
         if (componentObj instanceof Component current) {
            String raw = current.getString();
            if (raw.contains(profileName)) {
               return Component.literal(raw.replace(profileName, spoof)).withStyle(current.getStyle());
            }
         }

         if (componentObj == null) {
            return Component.literal(spoof);
         }
      } catch (Throwable var9) {
      }

      return componentObj;
   }

   public static boolean shouldMuteCrystalSound(Object soundObj) {
      try {
         return CrystalView.shouldMute((SoundInstance)soundObj);
      } catch (Throwable t) {
         return false;
      }
   }

   public static boolean shouldCancelExplosionParticles(Object centerObj) {
      try {
         Vec3 c = (Vec3)centerObj;
         return NoRender.hideExplosionParticlesAt(c.x, c.y, c.z);
      } catch (Throwable t) {
         return false;
      }
   }

   public static boolean shouldCancelAllExplosionParticles() {
      try {
         NoRender m = NoRender.INSTANCE;
         return m != null && m.shouldCancelExplosionParticles();
      } catch (Throwable t) {
         return false;
      }
   }

   public static boolean shouldCancelCrystalRender(Object stateObj) {
      try {
         EndCrystalRenderState s = (EndCrystalRenderState)stateObj;
         return s.beamOffset == null && CrystalView.active();
      } catch (Throwable t) {
         return false;
      }
   }

   public static boolean capeLayerIsInvisible(boolean original, Object stateObj) {
      try {
         if (!original) {
            return false;
         }

         Overtick ot = (Overtick)ModuleRegistry.get("Overtick");
         if (ot != null && ot.isGhostState((AvatarRenderState)stateObj)) {
            return false;
         }
      } catch (Throwable var3) {
      }

      return original;
   }

   public static void modelHandsBegin(Object layerObj, Object stateObj) {
      try {
         ModelHands.active = false;
         if (!PlayerModel.isModelState((ArmedEntityRenderState)stateObj)) {
            return;
         }

         Model model = ((RenderLayer)layerObj).getParentModel();
         ModelPart root = model.root();
         ModelHands.right = root.getChild("right_arm");
         ModelHands.left = root.getChild("left_arm");
         ModelHands.active = true;
      } catch (Throwable var4) {
      }
   }

   public static void modelHandsEnd() {
      try {
         ModelHands.active = false;
      } catch (Throwable var1) {
      }
   }

   public static boolean modelPartTranslateAndRotate(Object partObj, Object poseStackObj) {
      try {
         return !ModelHands.active ? false : ModelHands.apply((ModelPart)partObj, (PoseStack)poseStackObj);
      } catch (Throwable t) {
         return false;
      }
   }

   public static String safeFileName(String name) {
      return name == null ? "" : name;
   }

   public static void onPostChainLoaded(Object chainObj, Object idObj) {
      try {
         Identifier id = (Identifier)idObj;
         if (id == null || !"entity_outline".equals(id.getPath())) {
            return;
         }

         PostChain chain = (PostChain)chainObj;
         List<?> passes = null;

         for (Field f : chain.getClass().getDeclaredFields()) {
            if (List.class.isAssignableFrom(f.getType())) {
               f.setAccessible(true);
               passes = (List<?>)f.get(chain);
               break;
            }
         }

         if (passes != null) {
            ShaderEspRenderer.registerOutlinePasses(passes);
         }
      } catch (Throwable var9) {
      }
   }

   public static boolean onPostPassLambda(Object passObj) {
      try {
         return ShaderEspRenderer.onOutlinePass(passObj);
      } catch (Throwable t) {
         return false;
      }
   }

   public static void onProjectionGetBuffer(Object bufferObj, Object matrixOrProjection) {
      try {
         Minecraft mc = Minecraft.getInstance();
         if (mc == null || mc.gameRenderer == null) {
            return;
         }

         Object levelBuffer = null;

         for (Field f : mc.gameRenderer.getClass().getDeclaredFields()) {
            if (f.getType().getSimpleName().equals("ProjectionMatrixBuffer")) {
               f.setAccessible(true);
               levelBuffer = f.get(mc.gameRenderer);
               break;
            }
         }

         if (levelBuffer != bufferObj) {
            return;
         }

         if (matrixOrProjection instanceof Matrix4f m) {
            SkyShaderRenderer.captureProjection(m);
         } else if (matrixOrProjection instanceof Projection proj) {
            SkyShaderRenderer.captureProjection(proj.getMatrix(new Matrix4f()));
         }
      } catch (Throwable var8) {
      }
   }

   public static void themeRecolorSprite(Object idObj, Object imageObj) {
      try {
         Identifier id = (Identifier)idObj;
         if (id != null && ThemeTextures.shouldRecolor(id.getPath())) {
            ThemeTextures.recolor((NativeImage)imageObj);
         }
      } catch (Throwable var3) {
      }
   }

   public static void themeRecolorTexture(Object contentsObj, Object idObj) {
      try {
         TextureContents tc = (TextureContents)contentsObj;
         Identifier id = (Identifier)idObj;
         if (tc != null && id != null && ThemeTextures.shouldRecolor(id.getPath())) {
            ThemeTextures.recolor(tc.image());
         }
      } catch (Throwable var4) {
      }
   }

   public static boolean xcarryShouldCancel(Object playerObj) {
      try {
         LocalPlayer self = (LocalPlayer)playerObj;
         ModuleBase xcarry = ModuleRegistry.get("XCarry");
         return xcarry != null && xcarry.isEnabled() && self.containerMenu.containerId == 0;
      } catch (Throwable t) {
         return false;
      }
   }

   public static Object overrideSkin(Object playerInfoObj, Object skinObj) {
      try {
         Minecraft mc = Minecraft.getInstance();
         if (mc == null || mc.player == null) {
            return skinObj;
         }

         PlayerInfo pi = (PlayerInfo)playerInfoObj;
         if (!pi.getProfile().id().equals(mc.player.getUUID())) {
            return skinObj;
         }

         PlayerSkin s = SkinChanger.override((PlayerSkin)skinObj);
         if (s != null) {
            return s;
         }
      } catch (Throwable var5) {
      }

      return skinObj;
   }

   public static void animationsApplyAfterSwing(PoseStack poseStack) {
   }

   public static void moveAnimations(Object model, Object state) {
      try {
         MoveAnimations.apply((HumanoidModel<?>)model, (HumanoidRenderState)state);
      } catch (Throwable var3) {
      }
   }

   private static InputStream openClarity(ClassLoader sysCl, String entry) {
      try {
         InputStream is = sysCl.getResourceAsStream(entry);
         if (is == null && MiscBridge.class.getClassLoader() != null) {
            is = MiscBridge.class.getClassLoader().getResourceAsStream(entry);
         }

         if (is != null) {
            return is;
         }
      } catch (Throwable var10) {
      }

      try {
         String jarPath = System.getProperty("clarity.resources.jar");
         if (jarPath == null) {
            return null;
         }

         try (JarFile jf = new JarFile(new File(jarPath))) {
            JarEntry je = jf.getJarEntry(entry);
            if (je == null) {
               return null;
            }

            try (InputStream in = jf.getInputStream(je)) {
               return new ByteArrayInputStream(in.readAllBytes());
            }
         }
      } catch (Throwable t) {
         return null;
      }
   }

   private static boolean openClarityExists(ClassLoader sysCl, String entry) {
      try (InputStream is = openClarity(sysCl, entry)) {
         return is != null;
      } catch (Throwable t) {
         return false;
      }
   }

   private static void logRes(String what) {
      if (resLogCount++ < 12) {
         System.out.println("[Clarity] " + what);
      }
   }

   public static Object wrapResourceManager(Object realRm) {
      if (realRm == null) {
         return null;
      }

      if (Proxy.isProxyClass(realRm.getClass())) {
         return realRm;
      }

      if (realRm == lastRealRm && wrappedRm != null) {
         return wrappedRm;
      }

      try {
         Class<?> rmIface = findResourceManagerInterface(realRm.getClass());
         if (rmIface == null) {
            return realRm;
         }

         ClassLoader clRaw = realRm.getClass().getClassLoader();
         if (clRaw == null) {
            clRaw = ClassLoader.getSystemClassLoader();
         }

         ClassLoader cl = clRaw;
         Class<?> rlClass = null;

         for (String n : new String[]{"net.minecraft.resources.ResourceLocation", "net.minecraft.resources.Identifier"}) {
            try {
               rlClass = Class.forName(n, false, cl);
               break;
            } catch (ClassNotFoundException var10) {
            }
         }

         if (rlClass == null) {
            return realRm;
         }

         Class<?> rlFinal = rlClass;
         ClassLoader sysCl = ClassLoader.getSystemClassLoader();
         Object proxy = Proxy.newProxyInstance(cl, new Class[]{rmIface}, (proxyObj, method, args) -> {
            String mn = method.getName();
            if ((mn.equals("open") || mn.equals("getResource") || mn.equals("getResourceOrThrow")) && args != null && args.length >= 1 && rlFinal.isInstance(args[args.length - 1])) {
               Object rl = args[args.length - 1];
               String ns = (String)rlFinal.getMethod("getNamespace").invoke(rl);
               String path = (String)rlFinal.getMethod("getPath").invoke(rl);
               if ("clarity".equals(ns)) {
                  String resourcePath = "assets/clarity/" + path;
                  InputStream is = openClarity(sysCl, resourcePath);
                  logRes(mn + " clarity:" + path + " -> stream=" + (is != null));
                  if (is == null) {
                     is = MiscBridge.class.getClassLoader() != null ? MiscBridge.class.getClassLoader().getResourceAsStream(resourcePath) : null;
                  }

                  if (is != null) {
                     if (mn.equals("open")) {
                        return is;
                     }

                     return wrapAsResource(is, rl, realRm, method, args, cl);
                  }
               }
            }

            if (mn.equals("getNamespaces") && (args == null || args.length == 0)) {
               Set<String> out = new LinkedHashSet<>();

               try {
                  Object real = method.invoke(realRm);
                  if (real instanceof Collection) {
                     for (Object o : (Collection)real) {
                        out.add(String.valueOf(o));
                     }
                  }
               } catch (InvocationTargetException var17) {
               }

               boolean have = openClarityExists(sysCl, "assets/clarity/sounds.json");
               if (have) {
                  out.add("clarity");
               }

               if (!loggedNs) {
                  loggedNs = true;
                  System.out.println("[Clarity] getNamespaces: clarity sounds.json on classpath=" + have);
               }

               return out;
            } else if (mn.equals("getResourceStack") && args != null && args.length == 1 && rlFinal.isInstance(args[0])) {
               Object rl = args[0];
               List<Object> out = new ArrayList<>();

               try {
                  if (method.invoke(realRm, args) instanceof Collection<?> c) {
                     out.addAll((Collection<? extends Object>)c);
                  }
               } catch (InvocationTargetException var13x) {
               }

               if (out.isEmpty() && "clarity".equals(rlFinal.getMethod("getNamespace").invoke(rl))) {
                  String path = (String)rlFinal.getMethod("getPath").invoke(rl);
                  InputStream is = openClarity(sysCl, "assets/clarity/" + path);
                  if (is != null) {
                     Object res = buildInMemoryResource(is, rl, cl);
                     if (res != null) {
                        out.add(res);
                     }
                  }
               }

               return out;
            } else {
               if (mn.equals("listResources") && args != null) {
                  if (args.length == 2 && args[0] instanceof String prefix) {
                     Object predicate = args[1];

                     Map<Object, Object> result;
                     try {
                        Object realResult = method.invoke(realRm, args);
                        result = realResult != null ? new LinkedHashMap<>((Map<? extends Object, ? extends Object>)realResult) : new LinkedHashMap<>();
                     } catch (InvocationTargetException ite) {
                        result = new LinkedHashMap<>();
                     }

                     injectClarityResourcesIntoMap(prefix, predicate, result, rlFinal, sysCl, cl);
                     return result;
                  }

                  if (args.length >= 4) {
                     String ns = (String)args[1];
                     String subPath = (String)args[2];
                     Object consumer = args[3];
                     if ("clarity".equals(ns)) {
                        injectClarityResources(subPath, consumer, rlFinal, sysCl);
                     }

                     try {
                        method.invoke(realRm, args);
                     } catch (Throwable var15) {
                     }

                     return null;
                  }
               }

               try {
                  return method.invoke(realRm, args);
               } catch (InvocationTargetException ite) {
                  throw ite.getCause();
               }
            }
         });
         lastRealRm = realRm;
         wrappedRm = proxy;
         return proxy;
      } catch (Throwable t) {
         return realRm;
      }
   }

   private static Class<?> findResourceManagerInterface(Class<?> cls) {
      for (String name : new String[]{"net.minecraft.server.packs.resources.ResourceManager", "net.minecraft.resource.ResourceManager"}) {
         try {
            Class<?> iface = Class.forName(name, false, cls.getClassLoader() != null ? cls.getClassLoader() : ClassLoader.getSystemClassLoader());
            if (iface.isAssignableFrom(cls)) {
               return iface;
            }
         } catch (ClassNotFoundException var6) {
         }
      }

      Class<?>[] ifaces = cls.getInterfaces();
      return ifaces.length > 0 ? ifaces[0] : null;
   }

   private static void injectClarityResources(String subPath, Object consumer, Class<?> rlClass, ClassLoader sysCl) {
      try {
         String prefix = "assets/clarity/" + (subPath.isEmpty() ? "" : subPath + "/");
         String jarPath = System.getProperty("clarity.resources.jar");
         if (jarPath != null) {
            try (JarFile jf = new JarFile(new File(jarPath))) {
               Method accept = null;

               for (Method m : consumer.getClass().getMethods()) {
                  if (m.getName().equals("accept") && m.getParameterCount() == 2) {
                     accept = m;
                     break;
                  }
               }

               if (accept == null) {
                  return;
               }

               Method rlFactory = null;

               for (String n : new String[]{"fromNamespaceAndPath", "of"}) {
                  try {
                     rlFactory = rlClass.getDeclaredMethod(n, String.class, String.class);
                     break;
                  } catch (NoSuchMethodException var19) {
                  }
               }

               if (rlFactory != null) {
                  Method finalRlFactory = rlFactory;
                  Enumeration<JarEntry> entries = jf.entries();

                  while (entries.hasMoreElements()) {
                     JarEntry e = entries.nextElement();
                     if (!e.isDirectory() && e.getName().startsWith(prefix)) {
                        String entryPath = e.getName().substring("assets/clarity/".length());
                        Object rl = finalRlFactory.invoke(null, "clarity", entryPath);
                        String entryName = e.getName();
                        Object supplier = (Callable<InputStream>)() -> {
                           InputStream s = openClarity(sysCl, entryName);
                           if (s == null) {
                              s = MiscBridge.class.getClassLoader() != null ? MiscBridge.class.getClassLoader().getResourceAsStream(entryName) : null;
                           }

                           return s;
                        };

                        try {
                           accept.invoke(consumer, rl, supplier);
                        } catch (Throwable var18) {
                        }
                     }
                  }

                  return;
               }
            }
         }
      } catch (Throwable var21) {
      }
   }

   private static Object wrapAsResource(InputStream is, Object rl, Object realRm, Method method, Object[] args, ClassLoader cl) {
      try {
         Class<?> retType = method.getReturnType();
         if (retType == InputStream.class) {
            return is;
         }

         if (retType == Optional.class) {
            Object resource = buildInMemoryResource(is, rl, cl);
            if (resource == null) {
               logRes("wrapAsResource: buildInMemoryResource returned null for " + rl);
            }

            return resource != null ? Optional.of(resource) : Optional.empty();
         }

         Object resource = buildInMemoryResource(is, rl, cl);
         if (resource != null) {
            return resource;
         }
      } catch (Throwable var9) {
      }

      try {
         return method.invoke(realRm, args);
      } catch (Throwable t2) {
         return null;
      }
   }

   private static Object buildInMemoryResource(InputStream is, Object rl, ClassLoader cl) {
      try {
         String rlPath = null;

         try {
            rlPath = (String)rl.getClass().getMethod("getPath").invoke(rl);
         } catch (Throwable var14) {
         }

         String clarityPath = rlPath != null ? "assets/clarity/" + rlPath : null;
         ClassLoader sysCl = ClassLoader.getSystemClassLoader();
         Class<?> resourceClass = null;

         for (String n : new String[]{"net.minecraft.server.packs.resources.Resource", "net.minecraft.resource.Resource"}) {
            try {
               resourceClass = Class.forName(n, false, cl);
               break;
            } catch (ClassNotFoundException var15) {
            }
         }

         if (resourceClass == null) {
            return null;
         }

         if (resourceClass.isInterface()) {
            InputStream fis = is;
            Class<?> resIface = resourceClass;
            return Proxy.newProxyInstance(cl, new Class[]{resIface}, (px, m, a) -> {
               switch (m.getName()) {
                  case "open":
                  case "getInputStream":
                  case "openBuffered":
                     if (clarityPath != null) {
                        InputStream fresh = openClarity(sysCl, clarityPath);
                        if (fresh != null) {
                           return fresh;
                        }
                     }

                     return fis;
                  case "sourcePackId":
                  case "packId":
                  case "getSourcePackId":
                     return "clarity-runtime";
                  case "metadata":
                  case "getMetadata":
                     return null;
                  case "isBuiltin":
                     return true;
                  case "location":
                     return rl;
                  default:
                     return null;
               }
            });
         }

         ClassLoader fcl = cl;
         Constructor<?> best = null;

         for (Constructor<?> ctor : resourceClass.getDeclaredConstructors()) {
            Class<?>[] pt = ctor.getParameterTypes();
            if (pt.length >= 2 && pt.length <= 3 && pt[0].isInterface() && pt[1].isInterface() && (best == null || pt.length < best.getParameterCount())) {
               best = ctor;
            }
         }

         if (best != null) {
            best.setAccessible(true);
            Class<?>[] pt = best.getParameterTypes();
            Object pack = newStandInPack(pt[0], fcl);
            Object streamSupplier = Proxy.newProxyInstance(fcl, new Class[]{pt[1]}, (px, m2, a2) -> {
               if (m2.getDeclaringClass() == Object.class) {
                  return objectMethod(px, m2, a2);
               }

               if (clarityPath != null) {
                  InputStream fresh = openClarity(sysCl, clarityPath);
                  if (fresh != null) {
                     return fresh;
                  }
               }

               return is;
            });
            Object[] ctorArgs = new Object[pt.length];
            ctorArgs[0] = pack;
            ctorArgs[1] = streamSupplier;
            if (pt.length == 3) {
               Object emptyMeta = findEmptyMetadata(fcl);
               ctorArgs[2] = Proxy.newProxyInstance(fcl, new Class[]{pt[2]}, (px, m2, a2) -> m2.getDeclaringClass() == Object.class ? objectMethod(px, m2, a2) : emptyMeta);
            }

            return best.newInstance(ctorArgs);
         }

         System.out.println("[Clarity] buildInMemoryResource: no usable Resource constructor in " + Arrays.toString(resourceClass.getDeclaredConstructors()));
      } catch (Throwable t) {
         Throwable c = t instanceof InvocationTargetException && t.getCause() != null ? t.getCause() : t;
         if (!loggedBuildFail) {
            loggedBuildFail = true;
            System.out.println("[Clarity] buildInMemoryResource failed: " + c);
            c.printStackTrace(System.out);
         }
      }

      return null;
   }

   private static Object objectMethod(Object proxy, Method m, Object[] a) {
      switch (m.getName()) {
         case "hashCode":
            return System.identityHashCode(proxy);
         case "equals":
            return proxy == a[0];
         default:
            return "ClarityProxy";
      }
   }

   private static Object findEmptyMetadata(ClassLoader cl) {
      for (String n : new String[]{"net.minecraft.server.packs.resources.ResourceMetadata", "net.minecraft.server.packs.metadata.MetadataSectionType"}) {
         try {
            return Class.forName(n, false, cl).getField("EMPTY").get(null);
         } catch (Throwable var6) {
         }
      }

      return null;
   }

   private static Object newStandInPack(Class<?> packIface, ClassLoader cl) {
      Object info = buildPackLocationInfo(cl);
      return Proxy.newProxyInstance(cl, new Class[]{packIface}, (px, m, a) -> {
         if (m.getDeclaringClass() == Object.class) {
            return objectMethod(px, m, a);
         } else {
            String n = m.getName();
            Class<?> r = m.getReturnType();
            if (n.equals("packId") || n.equals("getName")) {
               return "clarity-runtime";
            } else if (n.equals("location") && info != null) {
               return info;
            } else if (r == boolean.class) {
               return n.equals("isBuiltin");
            } else if (r == int.class || r == long.class || r == short.class || r == byte.class) {
               return 0;
            } else if (r == Optional.class) {
               return Optional.empty();
            } else if (Set.class.isAssignableFrom(r)) {
               return Collections.emptySet();
            } else {
               return r == String.class ? "clarity-runtime" : null;
            }
         }
      });
   }

   private static Object buildPackLocationInfo(ClassLoader cl) {
      try {
         Class<?> pli = Class.forName("net.minecraft.server.packs.PackLocationInfo", false, cl);
         Class<?> comp = Class.forName("net.minecraft.network.chat.Component", false, cl);
         Class<?> src = Class.forName("net.minecraft.server.packs.repository.PackSource", false, cl);
         Object title = comp.getMethod("literal", String.class).invoke(null, "Clarity");
         Object source = src.getField("BUILT_IN").get(null);

         for (Constructor<?> c : pli.getDeclaredConstructors()) {
            if (c.getParameterCount() == 4) {
               c.setAccessible(true);
               return c.newInstance("clarity-runtime", title, source, Optional.empty());
            }
         }
      } catch (Throwable var10) {
      }

      return null;
   }

   private static void injectClarityResourcesIntoMap(String prefix, Object predicate, Map<Object, Object> result, Class<?> rlClass, ClassLoader sysCl, ClassLoader cl) {
      try {
         String jarPath = System.getProperty("clarity.resources.jar");
         if (jarPath != null) {
            Method rlFactory = null;

            for (String n : new String[]{"fromNamespaceAndPath", "of"}) {
               try {
                  rlFactory = rlClass.getDeclaredMethod(n, String.class, String.class);
                  break;
               } catch (NoSuchMethodException var24) {
               }
            }

            if (rlFactory != null) {
               Method testMethod = null;
               if (predicate != null) {
                  for (Method m : predicate.getClass().getMethods()) {
                     if (m.getName().equals("test") && m.getParameterCount() == 1) {
                        testMethod = m;
                        break;
                     }
                  }
               }

               Method finalFactory = rlFactory;
               Method finalTest = testMethod;
               String assetPrefix = "assets/clarity/" + (prefix.isEmpty() ? "" : prefix);

               try (JarFile jf = new JarFile(new File(jarPath))) {
                  Enumeration<JarEntry> entries = jf.entries();

                  while (true) {
                     String entryName;
                     Object rl;
                     while (true) {
                        while (true) {
                           if (!entries.hasMoreElements()) {
                              return;
                           }

                           JarEntry e = entries.nextElement();
                           if (!e.isDirectory()) {
                              entryName = e.getName();
                              if (entryName.startsWith("assets/clarity/")) {
                                 String relPath = entryName.substring("assets/clarity/".length());
                                 if (prefix.isEmpty() || relPath.startsWith(prefix)) {
                                    try {
                                       rl = finalFactory.invoke(null, "clarity", relPath);
                                       break;
                                    } catch (Throwable ignored2) {
                                    }
                                 }
                              }
                           }
                        }

                        if (finalTest == null) {
                           break;
                        }

                        try {
                           boolean ok = predicate instanceof Predicate ? ((Predicate)predicate).test(rl) : Boolean.TRUE.equals(finalTest.invoke(predicate, rl));
                           if (ok) {
                              break;
                           }
                        } catch (Throwable ignored2) {
                        }
                     }

                     if (!result.containsKey(rl)) {
                        InputStream is = openClarity(sysCl, entryName);
                        if (is == null) {
                           is = MiscBridge.class.getClassLoader() != null ? MiscBridge.class.getClassLoader().getResourceAsStream(entryName) : null;
                        }

                        if (is != null) {
                           Object resource = buildInMemoryResource(is, rl, cl);
                           if (resource != null) {
                              result.put(rl, resource);
                           }
                        }
                     }
                  }
               }
            }
         }
      } catch (Throwable var25) {
      }
   }
}
