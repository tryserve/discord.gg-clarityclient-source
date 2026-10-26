package need.clarity.agent;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import need.clarity.agent.asm.ClassReader;
import need.clarity.agent.asm.ClassWriter;
import need.clarity.agent.asm.Type;
import need.clarity.agent.asm.tree.AbstractInsnNode;
import need.clarity.agent.asm.tree.ClassNode;
import need.clarity.agent.asm.tree.FieldInsnNode;
import need.clarity.agent.asm.tree.InsnList;
import need.clarity.agent.asm.tree.InsnNode;
import need.clarity.agent.asm.tree.IntInsnNode;
import need.clarity.agent.asm.tree.JumpInsnNode;
import need.clarity.agent.asm.tree.LabelNode;
import need.clarity.agent.asm.tree.LdcInsnNode;
import need.clarity.agent.asm.tree.MethodInsnNode;
import need.clarity.agent.asm.tree.MethodNode;
import need.clarity.agent.asm.tree.TypeInsnNode;
import need.clarity.agent.asm.tree.VarInsnNode;

public class ClarityTransformer implements ClassFileTransformer {
   private static final Set<String> TARGETS = Set.of(
      "net/minecraft/network/Connection",
      "net/minecraft/client/player/LocalPlayer",
      "net/minecraft/client/player/KeyboardInput",
      "net/minecraft/client/Minecraft",
      "net/minecraft/world/entity/Entity",
      "net/minecraft/world/entity/player/Player",
      "net/minecraft/world/entity/LivingEntity",
      "net/minecraft/client/multiplayer/MultiPlayerGameMode",
      "net/minecraft/client/renderer/GameRenderer",
      "net/minecraft/world/level/Level",
      "net/minecraft/client/multiplayer/ClientLevel",
      "net/minecraft/client/renderer/fog/FogRenderer",
      "net/minecraft/client/renderer/LightmapRenderStateExtractor",
      "net/minecraft/client/renderer/entity/player/AvatarRenderer",
      "net/minecraft/client/renderer/SkyRenderer",
      "net/minecraft/client/gui/Gui",
      "net/minecraft/client/Camera",
      "net/minecraft/client/renderer/LevelRenderer",
      "net/minecraft/client/renderer/ScreenEffectRenderer",
      "net/minecraft/client/renderer/entity/EntityRenderer",
      "net/minecraft/client/renderer/entity/EntityRenderDispatcher",
      "net/minecraft/client/renderer/entity/LivingEntityRenderer",
      "net/minecraft/client/multiplayer/ClientCommonPacketListenerImpl",
      "net/minecraft/client/ClientBrandRetriever",
      "net/minecraft/client/renderer/chunk/CompiledSectionMesh",
      "net/minecraft/world/level/block/PowderSnowBlock",
      "net/minecraft/client/KeyboardHandler",
      "net/minecraft/client/MouseHandler",
      "net/minecraft/client/multiplayer/PlayerInfo",
      "net/minecraft/client/gui/components/ChatComponent",
      "net/minecraft/client/gui/screens/ChatScreen",
      "net/minecraft/client/gui/components/PlayerTabOverlay",
      "net/minecraft/client/renderer/ItemInHandRenderer",
      "net/minecraft/client/renderer/entity/EndCrystalRenderer",
      "net/minecraft/client/resources/sounds/SoundManager",
      "net/minecraft/client/sounds/SoundManager",
      "net/minecraft/client/renderer/ShaderManager",
      "net/minecraft/client/renderer/entity/layers/CapeLayer",
      "net/minecraft/client/renderer/entity/layers/ItemInHandLayer",
      "net/minecraft/client/model/geom/ModelPart",
      "net/minecraft/CrashReportCategory",
      "net/minecraft/client/renderer/PostChain",
      "net/minecraft/client/renderer/PostPass",
      "net/minecraft/client/renderer/ProjectionMatrixBuffer",
      "net/minecraft/client/multiplayer/ClientPacketListener",
      "net/minecraft/world/level/block/SlimeBlock",
      "net/minecraft/client/gui/components/ChatComponent$1",
      "net/minecraft/util/StringDecomposer",
      "net/minecraft/client/model/HumanoidModel"
   );

   public static boolean isTargetClass(String internalName) {
      return TARGETS.contains(internalName);
   }

   @Override
   public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain pd, byte[] bytes) {
      if (className != null && TARGETS.contains(className)) {
         try {
            byte[] result = switch (className) {
               case "net/minecraft/network/Connection" -> this.transformConnection(bytes, loader);
               case "net/minecraft/client/player/LocalPlayer" -> this.transformLocalPlayer(bytes, loader);
               case "net/minecraft/client/player/KeyboardInput" -> this.transformKeyboardInput(bytes, loader);
               case "net/minecraft/client/Minecraft" -> this.transformMinecraft(bytes, loader);
               case "net/minecraft/world/entity/Entity" -> this.transformEntity(bytes, loader);
               case "net/minecraft/world/entity/player/Player" -> this.transformPlayer(bytes, loader);
               case "net/minecraft/world/entity/LivingEntity" -> this.transformLivingEntity(bytes, loader);
               case "net/minecraft/client/multiplayer/MultiPlayerGameMode" -> this.transformMultiPlayerGameMode(bytes, loader);
               case "net/minecraft/client/renderer/GameRenderer" -> this.transformGameRenderer(bytes, loader);
               case "net/minecraft/world/level/Level" -> this.transformLevel(bytes, loader);
               case "net/minecraft/client/multiplayer/ClientLevel" -> this.transformClientLevel(bytes, loader);
               case "net/minecraft/client/renderer/fog/FogRenderer" -> this.transformFogRenderer(bytes, loader);
               case "net/minecraft/client/renderer/LightmapRenderStateExtractor" -> this.transformLightmapExtractor(bytes, loader);
               case "net/minecraft/client/renderer/entity/player/AvatarRenderer" -> this.transformAvatarRenderer(bytes, loader);
               case "net/minecraft/client/renderer/SkyRenderer" -> this.transformSkyRenderer(bytes, loader);
               case "net/minecraft/client/gui/Gui" -> this.transformGui(bytes, loader);
               case "net/minecraft/client/Camera" -> this.transformCamera(bytes, loader);
               case "net/minecraft/client/renderer/LevelRenderer" -> this.transformLevelRenderer(bytes, loader);
               case "net/minecraft/client/renderer/ScreenEffectRenderer" -> this.transformScreenEffectRenderer(bytes, loader);
               case "net/minecraft/client/renderer/entity/EntityRenderer" -> this.transformEntityRenderer(bytes, loader);
               case "net/minecraft/client/renderer/entity/EntityRenderDispatcher" -> this.transformEntityRenderDispatcher(bytes, loader);
               case "net/minecraft/client/renderer/entity/LivingEntityRenderer" -> this.transformLivingEntityRenderer(bytes, loader);
               case "net/minecraft/client/multiplayer/ClientCommonPacketListenerImpl" -> this.transformClientCommonPacketListener(bytes, loader);
               case "net/minecraft/client/ClientBrandRetriever" -> this.transformClientBrandRetriever(bytes, loader);
               case "net/minecraft/client/renderer/chunk/CompiledSectionMesh" -> this.transformCompiledSectionMesh(bytes, loader);
               case "net/minecraft/world/level/block/PowderSnowBlock" -> this.transformPowderSnowBlock(bytes, loader);
               case "net/minecraft/client/KeyboardHandler" -> this.transformKeyboardHandler(bytes, loader);
               case "net/minecraft/client/MouseHandler" -> this.transformMouseHandler(bytes, loader);
               case "net/minecraft/client/multiplayer/PlayerInfo" -> this.transformPlayerInfo(bytes, loader);
               case "net/minecraft/client/gui/components/ChatComponent" -> this.transformChatComponent(bytes, loader);
               case "net/minecraft/client/gui/screens/ChatScreen" -> this.transformChatScreen(bytes, loader);
               case "net/minecraft/client/gui/components/PlayerTabOverlay" -> this.transformPlayerTabOverlay(bytes, loader);
               case "net/minecraft/client/renderer/ItemInHandRenderer" -> this.transformItemInHandRenderer(bytes, loader);
               case "net/minecraft/client/renderer/entity/EndCrystalRenderer" -> this.transformEndCrystalRenderer(bytes, loader);
               case "net/minecraft/client/resources/sounds/SoundManager" -> this.wrapReloadResourceManager(this.transformSoundManager(bytes, loader), loader);
               case "net/minecraft/client/sounds/SoundManager", "net/minecraft/client/renderer/ShaderManager" -> this.wrapReloadResourceManager(bytes, loader);
               case "net/minecraft/client/renderer/entity/layers/CapeLayer" -> this.transformCapeLayer(bytes, loader);
               case "net/minecraft/client/renderer/entity/layers/ItemInHandLayer" -> this.transformItemInHandLayer(bytes, loader);
               case "net/minecraft/client/model/geom/ModelPart" -> this.transformModelPart(bytes, loader);
               case "net/minecraft/CrashReportCategory" -> this.transformCrashReportCategory(bytes, loader);
               case "net/minecraft/client/renderer/PostChain" -> this.transformPostChain(bytes, loader);
               case "net/minecraft/client/renderer/PostPass" -> this.transformPostPass(bytes, loader);
               case "net/minecraft/client/renderer/ProjectionMatrixBuffer" -> this.transformProjectionMatrixBuffer(bytes, loader);
               case "net/minecraft/client/multiplayer/ClientPacketListener" -> this.transformClientPacketListener(bytes, loader);
               case "net/minecraft/world/level/block/SlimeBlock" -> this.transformSlimeBlock(bytes, loader);
               case "net/minecraft/client/gui/components/ChatComponent$1" -> this.transformChatLineVisitor(bytes, loader);
               case "net/minecraft/util/StringDecomposer" -> this.transformStringDecomposer(bytes, loader);
               case "net/minecraft/client/model/HumanoidModel" -> this.transformHumanoidModel(bytes, loader);
               default -> null;
            };
            if (result != null) {
               System.out.println("[Clarity Agent] Transformed: " + className);
            }

            return result;
         } catch (Throwable t) {
            System.err.println("[Clarity Agent] FAILED to transform: " + className + " -> " + t);
            t.printStackTrace();
            return null;
         }
      } else {
         return null;
      }
   }

   private static ClassNode read(byte[] bytes) {
      ClassReader cr = new ClassReader(bytes);
      ClassNode cn = new ClassNode();
      cr.accept(cn, 8);
      return cn;
   }

   private static byte[] write(ClassNode cn, final ClassLoader gameLoader) {
      ClassWriter cw = new ClassWriter(2) {
         @Override
         protected String getCommonSuperClass(String type1, String type2) {
            ClassLoader cl = gameLoader != null ? gameLoader : ClassLoader.getSystemClassLoader();

            try {
               Class<?> c1 = Class.forName(type1.replace('/', '.'), false, cl);
               Class<?> c2 = Class.forName(type2.replace('/', '.'), false, cl);
               if (c1.isAssignableFrom(c2)) {
                  return type1;
               }

               if (c2.isAssignableFrom(c1)) {
                  return type2;
               }

               if (!c1.isInterface() && !c2.isInterface()) {
                  do {
                     c1 = c1.getSuperclass();
                  } while (!c1.isAssignableFrom(c2));

                  return c1.getName().replace('.', '/');
               } else {
                  return "java/lang/Object";
               }
            } catch (Throwable e) {
               return "java/lang/Object";
            }
         }
      };
      cn.accept(cw);
      return cw.toByteArray();
   }

   private static byte[] writeMaxs(ClassNode cn, final ClassLoader gameLoader) {
      ClassWriter cw = new ClassWriter(1) {
         @Override
         protected String getCommonSuperClass(String type1, String type2) {
            ClassLoader cl = gameLoader != null ? gameLoader : ClassLoader.getSystemClassLoader();

            try {
               Class<?> c1 = Class.forName(type1.replace('/', '.'), false, cl);
               Class<?> c2 = Class.forName(type2.replace('/', '.'), false, cl);
               if (c1.isAssignableFrom(c2)) {
                  return type1;
               }

               if (c2.isAssignableFrom(c1)) {
                  return type2;
               }

               if (!c1.isInterface() && !c2.isInterface()) {
                  do {
                     c1 = c1.getSuperclass();
                  } while (!c1.isAssignableFrom(c2));

                  return c1.getName().replace('.', '/');
               } else {
                  return "java/lang/Object";
               }
            } catch (Throwable e) {
               return "java/lang/Object";
            }
         }
      };
      cn.accept(cw);
      return cw.toByteArray();
   }

   private static MethodNode findMethod(ClassNode cn, String name) {
      for (MethodNode m : cn.methods) {
         if (m.name.equals(name)) {
            return m;
         }
      }

      return null;
   }

   private static MethodNode findMethod(ClassNode cn, String name, String desc) {
      for (MethodNode m : cn.methods) {
         if (m.name.equals(name) && m.desc.equals(desc)) {
            return m;
         }
      }

      return null;
   }

   private static void insertHeadCall(MethodNode mn, String bridge, String method, String desc) {
      InsnList p = new InsnList();
      p.add(new MethodInsnNode(184, bridge, method, desc, false));
      mn.instructions.insertBefore(mn.instructions.getFirst(), p);
   }

   private static void insertCancellableHead(MethodNode mn, String bridge, String method, String desc) {
      InsnList p = new InsnList();
      LabelNode cont = new LabelNode();
      p.add(new MethodInsnNode(184, bridge, method, desc, false));
      p.add(new JumpInsnNode(153, cont));
      p.add(new InsnNode(177));
      p.add(cont);
      mn.instructions.insertBefore(mn.instructions.getFirst(), p);
   }

   private static List<AbstractInsnNode> findReturns(MethodNode mn) {
      List<AbstractInsnNode> ret = new ArrayList<>();

      for (AbstractInsnNode insn : mn.instructions) {
         int op = insn.getOpcode();
         if (op >= 172 && op <= 177) {
            ret.add(insn);
         }
      }

      return ret;
   }

   private static int argSlot(MethodNode mn, int argIndex) {
      int slot = (mn.access & 8) != 0 ? 0 : 1;
      Type[] args = Type.getArgumentTypes(mn.desc);

      for (int i = 0; i < argIndex; i++) {
         slot += args[i].getSize();
      }

      return slot;
   }

   private static void wrapFloatReturn(MethodNode mn, String bridge, String method) {
      for (AbstractInsnNode ret : findReturns(mn)) {
         if (ret.getOpcode() == 174) {
            mn.instructions.insertBefore(ret, new MethodInsnNode(184, bridge, method, "(F)F", false));
         }
      }
   }

   private byte[] transformConnection(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("genericsFtw")) {
            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new VarInsnNode(25, 0));
            p.add(new VarInsnNode(25, 1));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/ConnectionBridge", "onPacketReceive", "(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)Z", false));
            p.add(new JumpInsnNode(153, cont));
            p.add(new InsnNode(177));
            p.add(cont);
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("send") && mn.desc.startsWith("(Lnet/minecraft/network/protocol/Packet;")) {
            InsnList p = new InsnList();
            p.add(new VarInsnNode(25, 1));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/ConnectionBridge", "onConnectionSend", "(Lnet/minecraft/network/protocol/Packet;)V", false));
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformLocalPlayer(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);
      MethodNode tick = findMethod(cn, "tick", "()V");
      if (tick != null) {
         insertHeadCall(tick, "need/clarity/bridge/LocalPlayerBridge", "onTickHead", "()V");

         for (AbstractInsnNode ret : findReturns(tick)) {
            InsnList p = new InsnList();
            p.add(new MethodInsnNode(184, "need/clarity/bridge/LocalPlayerBridge", "onTickReturn", "()V", false));
            tick.instructions.insertBefore(ret, p);
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("closeContainer")) {
            for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
               if (insn instanceof MethodInsnNode min && min.name.equals("send") && min.owner.contains("ClientPacketListener")) {
                  InsnList p = new InsnList();
                  LabelNode doSend = new LabelNode();
                  LabelNode afterSend = new LabelNode();
                  p.add(new VarInsnNode(25, 0));
                  p.add(new MethodInsnNode(184, "need/clarity/bridge/MiscBridge", "xcarryShouldCancel", "(Ljava/lang/Object;)Z", false));
                  p.add(new JumpInsnNode(153, doSend));
                  p.add(new InsnNode(88));
                  p.add(new JumpInsnNode(167, afterSend));
                  p.add(doSend);
                  mn.instructions.insertBefore(insn, p);
                  InsnList after = new InsnList();
                  after.add(afterSend);
                  mn.instructions.insert(insn, after);
                  return write(cn, cl);
               }
            }
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformKeyboardInput(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);
      MethodNode tick = findMethod(cn, "tick");
      if (tick != null) {
         insertHeadCall(tick, "need/clarity/bridge/KeyboardInputBridge", "onTickHead", "()V");

         for (AbstractInsnNode ret : findReturns(tick)) {
            InsnList p = new InsnList();
            p.add(new MethodInsnNode(184, "need/clarity/bridge/KeyboardInputBridge", "onTickReturn", "()V", false));
            tick.instructions.insertBefore(ret, p);
         }
      }

      return write(cn, cl);
   }

   private byte[] transformMinecraft(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("startAttack")) {
            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new VarInsnNode(25, 0));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/MinecraftBridge", "onStartAttack", "(Lnet/minecraft/client/Minecraft;)Z", false));
            p.add(new JumpInsnNode(153, cont));
            p.add(new InsnNode(3));
            p.add(new InsnNode(172));
            p.add(cont);
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("handleKeybinds")) {
            InsnList p = new InsnList();
            p.add(new VarInsnNode(25, 0));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/MinecraftBridge", "onHandleKeybinds", "(Lnet/minecraft/client/Minecraft;)V", false));
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("getResourceManager")) {
            for (AbstractInsnNode ret : findReturns(mn)) {
               if (ret.getOpcode() == 176) {
                  InsnList p = new InsnList();
                  p.add(new MethodInsnNode(184, "need/clarity/bridge/MiscBridge", "wrapResourceManager", "(Ljava/lang/Object;)Ljava/lang/Object;", false));
                  p.add(new TypeInsnNode(192, "net/minecraft/server/packs/resources/ResourceManager"));
                  mn.instructions.insertBefore(ret, p);
               }
            }
            break;
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("setScreen")) {
            InsnList agc = new InsnList();
            LabelNode agcCont = new LabelNode();
            agc.add(new VarInsnNode(25, 0));
            agc.add(new VarInsnNode(25, 1));
            agc.add(new MethodInsnNode(184, "need/clarity/bridge/GameplayBridge", "antiGuiCloseSetScreen", "(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/Screen;)Z", false));
            agc.add(new JumpInsnNode(153, agcCont));
            agc.add(new InsnNode(177));
            agc.add(agcCont);
            InsnList p = new InsnList();
            p.add(new VarInsnNode(25, 1));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/MinecraftBridge", "redirectScreen", "(Lnet/minecraft/client/gui/screens/Screen;)Lnet/minecraft/client/gui/screens/Screen;", false));
            p.add(new VarInsnNode(58, 1));
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            mn.instructions.insertBefore(mn.instructions.getFirst(), agc);

            for (AbstractInsnNode ret : findReturns(mn)) {
               InsnList tp = new InsnList();
               tp.add(new VarInsnNode(25, 0));
               tp.add(new VarInsnNode(25, 1));
               tp.add(new MethodInsnNode(184, "need/clarity/bridge/MinecraftBridge", "onSetScreenReturn", "(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/Screen;)V", false));
               mn.instructions.insertBefore(ret, tp);
            }
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformEntity(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if ((mn.name.equals("getBlockSpeedFactor") || mn.name.equals("getBlockJumpFactor")) && mn.desc.equals("()F")) {
            boolean speed = mn.name.equals("getBlockSpeedFactor");

            for (AbstractInsnNode ret : findReturns(mn)) {
               if (ret.getOpcode() == 174) {
                  InsnList p = new InsnList();
                  p.add(new VarInsnNode(25, 0));
                  p.add(new MethodInsnNode(184, "need/clarity/bridge/GameplayBridge", speed ? "noSlowSpeed" : "noSlowJump", "(FLnet/minecraft/world/entity/Entity;)F", false));
                  mn.instructions.insertBefore(ret, p);
               }
            }
         } else if (mn.name.equals("makeStuckInBlock") && mn.desc.equals("(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/Vec3;)V")) {
            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new VarInsnNode(25, 0));
            p.add(new VarInsnNode(25, 1));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/GameplayBridge", "noSlowStuck", "(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;)Z", false));
            p.add(new JumpInsnNode(153, cont));
            p.add(new InsnNode(177));
            p.add(cont);
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("push") && mn.desc.equals("(DDD)V")) {
            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new VarInsnNode(25, 0));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/EntityBridge", "onPush", "(Lnet/minecraft/world/entity/Entity;)Z", false));
            p.add(new JumpInsnNode(153, cont));
            p.add(new InsnNode(177));
            p.add(cont);
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("getViewVector")) {
            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new VarInsnNode(25, 0));
            p.add(new VarInsnNode(23, 1));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/EntityBridge", "getViewVectorOverride", "(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/world/phys/Vec3;", false));
            p.add(new InsnNode(89));
            p.add(new JumpInsnNode(198, cont));
            p.add(new InsnNode(176));
            p.add(cont);
            p.add(new InsnNode(87));
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("getHeadLookAngle")) {
            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new VarInsnNode(25, 0));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/EntityBridge", "getHeadLookAngleOverride", "(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/phys/Vec3;", false));
            p.add(new InsnNode(89));
            p.add(new JumpInsnNode(198, cont));
            p.add(new InsnNode(176));
            p.add(cont);
            p.add(new InsnNode(87));
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("getEyeHeight") && mn.desc.equals("()F")) {
            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new VarInsnNode(25, 0));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/EntityBridge", "getEyeHeightOverride", "(Lnet/minecraft/world/entity/Entity;)F", false));
            p.add(new InsnNode(89));
            p.add(new LdcInsnNode(0.0F));
            p.add(new InsnNode(150));
            p.add(new JumpInsnNode(155, cont));
            p.add(new InsnNode(174));
            p.add(cont);
            p.add(new InsnNode(87));
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformPlayer(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         boolean block = mn.name.equals("blockInteractionRange") && mn.desc.equals("()D");
         boolean entity = mn.name.equals("entityInteractionRange") && mn.desc.equals("()D");
         if (block || entity) {
            InsnList p = new InsnList();
            LabelNode skip = new LabelNode();
            p.add(new VarInsnNode(25, 0));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/PlayerBridge", block ? "blockReach" : "entityReach", "(Lnet/minecraft/world/entity/player/Player;)D", false));
            p.add(new InsnNode(92));
            p.add(new InsnNode(14));
            p.add(new InsnNode(152));
            p.add(new JumpInsnNode(158, skip));
            p.add(new InsnNode(175));
            p.add(skip);
            p.add(new InsnNode(88));
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
         }
      }

      return write(cn, cl);
   }

   private byte[] transformLivingEntity(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("swing") && mn.desc.contains("InteractionHand")) {
            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new VarInsnNode(25, 0));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/LivingEntityBridge", "shouldCancelSwing", "(Lnet/minecraft/world/entity/LivingEntity;)Z", false));
            p.add(new JumpInsnNode(153, cont));
            p.add(new InsnNode(177));
            p.add(cont);
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("travel")) {
            InsnList nsP = new InsnList();
            nsP.add(new VarInsnNode(25, 0));
            nsP.add(new VarInsnNode(25, 1));
            nsP.add(
               new MethodInsnNode(
                  184, "need/clarity/bridge/LivingEntityBridge", "noSlowTravel", "(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", false
               )
            );
            nsP.add(new VarInsnNode(58, 1));
            mn.instructions.insertBefore(mn.instructions.getFirst(), nsP);
            InsnList headP = new InsnList();
            headP.add(new VarInsnNode(25, 0));
            headP.add(new VarInsnNode(25, 1));
            headP.add(new MethodInsnNode(184, "need/clarity/bridge/LivingEntityBridge", "onTravelHead", "(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/phys/Vec3;)V", false));
            mn.instructions.insertBefore(mn.instructions.getFirst(), headP);

            for (AbstractInsnNode ret : findReturns(mn)) {
               InsnList tp = new InsnList();
               tp.add(new VarInsnNode(25, 0));
               tp.add(new MethodInsnNode(184, "need/clarity/bridge/LivingEntityBridge", "onTravelReturn", "(Lnet/minecraft/world/entity/LivingEntity;)V", false));
               mn.instructions.insertBefore(ret, tp);
            }
            break;
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("causeFallDamage")) {
            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new VarInsnNode(25, 0));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/LivingEntityBridge", "shouldCancelFallDamage", "(Lnet/minecraft/world/entity/LivingEntity;)Z", false));
            p.add(new JumpInsnNode(153, cont));
            p.add(new InsnNode(3));
            p.add(new InsnNode(172));
            p.add(cont);
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("getCurrentSwingDuration")) {
            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new VarInsnNode(25, 0));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/LivingEntityBridge", "getSwingDurationOverride", "(Lnet/minecraft/world/entity/LivingEntity;)I", false));
            p.add(new InsnNode(89));
            p.add(new JumpInsnNode(155, cont));
            p.add(new InsnNode(172));
            p.add(cont);
            p.add(new InsnNode(87));
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformMultiPlayerGameMode(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("attack") && mn.desc.equals("(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;)V")) {
            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new VarInsnNode(25, 1));
            p.add(new VarInsnNode(25, 2));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/MultiPlayerGameModeBridge", "attackHead", "(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;)Z", false));
            p.add(new JumpInsnNode(153, cont));
            p.add(new InsnNode(177));
            p.add(cont);
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("useItem")) {
            int savedSlot = mn.maxLocals++;
            InsnList headP = new InsnList();
            headP.add(new VarInsnNode(25, 1));
            headP.add(new MethodInsnNode(184, "need/clarity/bridge/MultiPlayerGameModeBridge", "useItemHead", "(Lnet/minecraft/world/entity/player/Player;)[F", false));
            headP.add(new VarInsnNode(58, savedSlot));
            mn.instructions.insertBefore(mn.instructions.getFirst(), headP);

            for (AbstractInsnNode ret : findReturns(mn)) {
               InsnList retP = new InsnList();
               retP.add(new VarInsnNode(25, 1));
               retP.add(new VarInsnNode(25, savedSlot));
               retP.add(new MethodInsnNode(184, "need/clarity/bridge/MultiPlayerGameModeBridge", "useItemReturn", "(Lnet/minecraft/world/entity/player/Player;[F)V", false));
               mn.instructions.insertBefore(ret, retP);
            }
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformGameRenderer(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("renderLevel")) {
            insertHeadCall(mn, "need/clarity/bridge/GameRendererBridge", "onRenderLevelHead", "()V");

            for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
               if (insn instanceof MethodInsnNode min && min.name.equals("renderItemInHand")) {
                  InsnList before = new InsnList();
                  before.add(new MethodInsnNode(184, "need/clarity/bridge/GameRendererBridge", "beforeRenderHand", "()V", false));
                  mn.instructions.insertBefore(insn, before);
                  InsnList after = new InsnList();
                  after.add(new MethodInsnNode(184, "need/clarity/bridge/GameRendererBridge", "afterRenderHand", "()V", false));
                  mn.instructions.insert(insn, after);
                  break;
               }
            }

            for (AbstractInsnNode ret : findReturns(mn)) {
               InsnList p = new InsnList();
               p.add(new MethodInsnNode(184, "need/clarity/bridge/GameRendererBridge", "captureClickGuiScene", "()V", false));
               mn.instructions.insertBefore(ret, p);
            }
            break;
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("bobHurt")) {
            insertCancellableHead(mn, "need/clarity/bridge/GameRendererBridge", "shouldCancelBobHurt", "()Z");
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformLevel(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (String mName : new String[]{"getOverworldClockTime", "getDefaultClockTime"}) {
         for (MethodNode mn : cn.methods) {
            if (mn.name.equals(mName) && mn.desc.endsWith("J")) {
               for (AbstractInsnNode ret : findReturns(mn)) {
                  InsnList p = new InsnList();
                  p.add(new VarInsnNode(25, 0));
                  p.add(new TypeInsnNode(193, "net/minecraft/client/multiplayer/ClientLevel"));
                  p.add(new MethodInsnNode(184, "need/clarity/bridge/ClientLevelAmbienceBridge", "overrideClockTime", "(JZ)J", false));
                  mn.instructions.insertBefore(ret, p);
               }
               break;
            }
         }
      }

      for (String mName : new String[]{"getRainLevel", "getThunderLevel"}) {
         String bridgeMethod = mName.equals("getRainLevel") ? "overrideRainLevel" : "overrideThunderLevel";

         for (MethodNode mn : cn.methods) {
            if (mn.name.equals(mName) && mn.desc.contains("F")) {
               for (AbstractInsnNode ret : findReturns(mn)) {
                  if (ret.getOpcode() == 174) {
                     InsnList p = new InsnList();
                     p.add(new VarInsnNode(25, 0));
                     p.add(new TypeInsnNode(193, "net/minecraft/client/multiplayer/ClientLevel"));
                     p.add(new MethodInsnNode(184, "need/clarity/bridge/ClientLevelAmbienceBridge", bridgeMethod, "(FZ)F", false));
                     mn.instructions.insertBefore(ret, p);
                  }
               }
               break;
            }
         }
      }

      return write(cn, cl);
   }

   private byte[] transformClientLevel(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("trackExplosionEffects")) {
            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new VarInsnNode(25, 1));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/MiscBridge", "shouldCancelExplosionParticles", "(Ljava/lang/Object;)Z", false));
            p.add(new JumpInsnNode(153, cont));
            p.add(new InsnNode(177));
            p.add(cont);
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("explode") && mn.desc.contains("ExplosionInteraction")) {
            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new MethodInsnNode(184, "need/clarity/bridge/MiscBridge", "shouldCancelAllExplosionParticles", "()Z", false));
            p.add(new JumpInsnNode(153, cont));
            p.add(new InsnNode(177));
            p.add(cont);
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformFogRenderer(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("setupFog")) {
            for (AbstractInsnNode ret : findReturns(mn)) {
               if (ret.getOpcode() == 176) {
                  InsnList p = new InsnList();
                  p.add(new InsnNode(89));
                  p.add(new MethodInsnNode(184, "need/clarity/bridge/FogAmbienceBridge", "onSetupFogReturn", "(Ljava/lang/Object;)V", false));
                  mn.instructions.insertBefore(ret, p);
               }
            }
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformLightmapExtractor(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("extract")) {
            for (AbstractInsnNode ret : findReturns(mn)) {
               InsnList p = new InsnList();
               p.add(new VarInsnNode(25, 1));
               p.add(new MethodInsnNode(184, "need/clarity/bridge/LightmapAmbienceBridge", "onExtractTail", "(Ljava/lang/Object;)V", false));
               mn.instructions.insertBefore(ret, p);
            }
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformAvatarRenderer(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("extractRenderState")) {
            for (AbstractInsnNode ret : findReturns(mn)) {
               InsnList p = new InsnList();
               p.add(new VarInsnNode(25, 1));
               p.add(new VarInsnNode(25, 2));
               p.add(new VarInsnNode(23, 3));
               p.add(new MethodInsnNode(184, "need/clarity/bridge/AvatarRendererBridge", "onExtractRenderStateTail", "(Ljava/lang/Object;Ljava/lang/Object;F)V", false));
               mn.instructions.insertBefore(ret, p);
            }
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformSkyRenderer(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);
      String[][] skyCancels = new String[][]{
         {"renderSkyDisc", "need/clarity/render/sky/SkyShaderRenderer", "tryRender", "(Z)Z", "0"},
         {"renderEndSky", "need/clarity/render/sky/SkyShaderRenderer", "tryRender", "(Z)Z", "1"},
         {"renderDarkDisc", "need/clarity/render/sky/SkyShaderRenderer", "isActiveThisFrame", "()Z", null},
         {"renderSunriseAndSunset", "need/clarity/render/sky/SkyShaderRenderer", "isActiveThisFrame", "()Z", null},
         {"renderStars", "need/clarity/render/sky/SkyShaderRenderer", "isActiveThisFrame", "()Z", null},
         {"renderEndFlash", "need/clarity/render/sky/SkyShaderRenderer", "isActiveThisFrame", "()Z", null}
      };

      for (String[] entry : skyCancels) {
         for (MethodNode mn : cn.methods) {
            if (mn.name.equals(entry[0])) {
               InsnList p = new InsnList();
               LabelNode cont = new LabelNode();
               if (entry[4] != null) {
                  p.add(new InsnNode(entry[4].equals("1") ? 4 : 3));
               }

               p.add(new MethodInsnNode(184, entry[1], entry[2], entry[3], false));
               p.add(new JumpInsnNode(153, cont));
               p.add(new InsnNode(177));
               p.add(cont);
               mn.instructions.insertBefore(mn.instructions.getFirst(), p);
               break;
            }
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("renderSunMoonAndStars")) {
            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new MethodInsnNode(184, "need/clarity/render/sky/SkyShaderRenderer", "isActiveThisFrame", "()Z", false));
            p.add(new JumpInsnNode(153, cont));
            p.add(new MethodInsnNode(184, "need/clarity/modules/visual/SkyShaders", "keepSunMoon", "()Z", false));
            p.add(new JumpInsnNode(154, cont));
            p.add(new InsnNode(177));
            p.add(cont);
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("extractRenderState")) {
            for (AbstractInsnNode ret : findReturns(mn)) {
               InsnList p = new InsnList();
               p.add(new VarInsnNode(25, 1));
               p.add(new VarInsnNode(23, 2));
               p.add(new VarInsnNode(25, 3));
               p.add(new VarInsnNode(25, 4));
               p.add(new MethodInsnNode(184, "need/clarity/bridge/SkyAmbienceBridge", "onExtractSkyTail", "(Ljava/lang/Object;FLjava/lang/Object;Ljava/lang/Object;)V", false));
               mn.instructions.insertBefore(ret, p);
            }
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformGui(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("displayScoreboardSidebar")) {
            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new VarInsnNode(25, 1));
            p.add(new VarInsnNode(25, 2));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/ChatBridge", "customScoreboard", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false));
            p.add(new JumpInsnNode(153, cont));
            p.add(new InsnNode(177));
            p.add(cont);
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("extractCrosshair")) {
            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new VarInsnNode(25, 1));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/GuiBridge", "onExtractCrosshair", "(Ljava/lang/Object;)Z", false));
            p.add(new JumpInsnNode(153, cont));
            p.add(new InsnNode(177));
            p.add(cont);
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      String[][] noRender = new String[][]{
         {"extractConfusionOverlay", "shouldCancelNausea"},
         {"extractPortalOverlay", "shouldCancelPortal"},
         {"extractSpyglassOverlay", "shouldCancelSpyglass"},
         {"extractScoreboardSidebar", "shouldCancelScoreboard"},
         {"extractVignette", "shouldCancelVignette"}
      };

      for (String[] pair : noRender) {
         for (MethodNode mn : cn.methods) {
            if (mn.name.equals(pair[0])) {
               insertCancellableHead(mn, "need/clarity/bridge/GuiBridge", pair[1], "()Z");
               break;
            }
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("extractRenderState")) {
            for (AbstractInsnNode ret : findReturns(mn)) {
               InsnList p = new InsnList();
               p.add(new VarInsnNode(25, 1));
               p.add(new MethodInsnNode(184, "need/clarity/bridge/GuiBridge", "onArrayListOverlay", "(Ljava/lang/Object;)V", false));
               mn.instructions.insertBefore(ret, p);
            }
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformCamera(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("calculateFov") && mn.desc.equals("(F)F")) {
            wrapFloatReturn(mn, "need/clarity/bridge/CameraBridge", "zoomFov");
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("alignWithEntity")) {
            for (AbstractInsnNode insn : mn.instructions.toArray()) {
               if (insn instanceof MethodInsnNode min && min.name.equals("setRotation") && min.desc.equals("(FF)V")) {
                  mn.instructions.set(insn, new MethodInsnNode(184, "need/clarity/bridge/CameraBridge", "freelookSetRotation", "(Lnet/minecraft/client/Camera;FF)V", false));
               }
            }
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("extractRenderState")) {
            for (AbstractInsnNode ret : findReturns(mn)) {
               InsnList p = new InsnList();
               p.add(new VarInsnNode(25, 1));
               p.add(new VarInsnNode(23, 2));
               p.add(new MethodInsnNode(184, "need/clarity/bridge/CameraBridge", "onExtractRenderStateTail", "(Ljava/lang/Object;F)V", false));
               mn.instructions.insertBefore(ret, p);
            }
            break;
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("update") && mn.desc.contains("DeltaTracker")) {
            for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
               if (insn instanceof MethodInsnNode min && min.name.equals("alignWithEntity")) {
                  InsnList p = new InsnList();
                  p.add(new VarInsnNode(25, 0));
                  p.add(new MethodInsnNode(184, "need/clarity/bridge/CameraBridge", "onAfterAlignWithEntity", "(Ljava/lang/Object;)V", false));
                  mn.instructions.insert(insn, p);
                  break;
               }
            }

            for (AbstractInsnNode ret : findReturns(mn)) {
               InsnList p = new InsnList();
               p.add(new VarInsnNode(25, 0));
               p.add(new VarInsnNode(25, 1));
               p.add(new MethodInsnNode(184, "need/clarity/bridge/CameraBridge", "onUpdateReturn", "(Ljava/lang/Object;Ljava/lang/Object;)V", false));
               mn.instructions.insertBefore(ret, p);
            }
            break;
         }
      }

      label92:
      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("alignWithEntity")) {
            for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
               if (insn instanceof MethodInsnNode min && min.name.equals("getMaxZoom")) {
                  InsnList smooth = new InsnList();
                  smooth.add(new VarInsnNode(25, 0));
                  smooth.add(new MethodInsnNode(184, "need/clarity/bridge/CameraBridge", "onBeforeGetMaxZoom", "(Ljava/lang/Object;)V", false));
                  mn.instructions.insertBefore(insn, smooth);
                  InsnList tweaks = new InsnList();
                  LabelNode vanilla = new LabelNode();
                  tweaks.add(new VarInsnNode(25, 0));
                  tweaks.add(new MethodInsnNode(184, "need/clarity/bridge/CameraBridge", "cameraTweaks", "(Ljava/lang/Object;)Z", false));
                  tweaks.add(new JumpInsnNode(153, vanilla));
                  tweaks.add(new InsnNode(177));
                  tweaks.add(vanilla);
                  mn.instructions.insertBefore(insn, tweaks);
                  break label92;
               }
            }
            break;
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("position") && mn.desc.endsWith("Vec3;")) {
            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new MethodInsnNode(184, "need/clarity/bridge/CameraBridge", "getFreecamPosition", "()Lnet/minecraft/world/phys/Vec3;", false));
            p.add(new InsnNode(89));
            p.add(new JumpInsnNode(198, cont));
            p.add(new InsnNode(176));
            p.add(cont);
            p.add(new InsnNode(87));
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformLevelRenderer(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("renderLevel") && mn.desc.contains("DeltaTracker")) {
            InsnList skyHead = new InsnList();
            skyHead.add(new VarInsnNode(25, 5));
            skyHead.add(new VarInsnNode(25, 4));
            skyHead.add(new MethodInsnNode(184, "need/clarity/bridge/LevelRendererBridge", "onSkyBeginFrame", "(Ljava/lang/Object;Ljava/lang/Object;)V", false));
            mn.instructions.insertBefore(mn.instructions.getFirst(), skyHead);
            break;
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("addCloudsPass")) {
            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new MethodInsnNode(184, "need/clarity/modules/visual/SkyShaders", "hideClouds", "()Z", false));
            p.add(new JumpInsnNode(153, cont));
            p.add(new InsnNode(177));
            p.add(cont);
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("doEntityOutline")) {
            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new MethodInsnNode(184, "need/clarity/render/esp/ShaderEspRenderer", "onDoEntityOutline", "()Z", false));
            p.add(new JumpInsnNode(153, cont));
            p.add(new InsnNode(177));
            p.add(cont);
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("renderLevel") && mn.desc.contains("DeltaTracker")) {
            for (AbstractInsnNode ret : findReturns(mn)) {
               InsnList p = new InsnList();
               p.add(new VarInsnNode(25, 2));
               p.add(new VarInsnNode(25, 5));
               p.add(new MethodInsnNode(184, "need/clarity/bridge/LevelRendererBridge", "onRenderLevelReturn", "(Lnet/minecraft/client/DeltaTracker;Lorg/joml/Matrix4fc;)V", false));
               mn.instructions.insertBefore(ret, p);
            }
            break;
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("extractVisibleEntities")) {
            for (AbstractInsnNode ret : findReturns(mn)) {
               InsnList p = new InsnList();
               p.add(new VarInsnNode(25, 4));
               p.add(new VarInsnNode(25, 3));
               p.add(new InsnNode(3));
               p.add(new MethodInsnNode(185, "net/minecraft/client/DeltaTracker", "getGameTimeDeltaPartialTick", "(Z)F", true));
               p.add(new MethodInsnNode(184, "need/clarity/bridge/LevelRendererBridge", "onExtractVisibleEntitiesTail", "(Ljava/lang/Object;F)V", false));
               mn.instructions.insertBefore(ret, p);
            }
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformScreenEffectRenderer(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("renderFire")) {
            insertCancellableHead(mn, "need/clarity/bridge/ScreenEffectRendererBridge", "shouldCancelFire", "()Z");
            break;
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("renderItemActivationAnimation")) {
            insertCancellableHead(mn, "need/clarity/bridge/ScreenEffectRendererBridge", "shouldCancelTotemAnimation", "()Z");
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformEntityRenderer(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("extractRenderState")) {
            for (AbstractInsnNode ret : findReturns(mn)) {
               InsnList p = new InsnList();
               p.add(new VarInsnNode(25, 1));
               p.add(new VarInsnNode(25, 2));
               p.add(new VarInsnNode(23, 3));
               p.add(new MethodInsnNode(184, "need/clarity/bridge/EntityRendererBridge", "onExtractRenderStateTail", "(Ljava/lang/Object;Ljava/lang/Object;F)V", false));
               mn.instructions.insertBefore(ret, p);
            }
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformEntityRenderDispatcher(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("shouldRender")) {
            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new VarInsnNode(25, 1));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/EntityRendererBridge", "shouldHideCorpse", "(Ljava/lang/Object;)Z", false));
            p.add(new JumpInsnNode(153, cont));
            p.add(new InsnNode(3));
            p.add(new InsnNode(172));
            p.add(cont);
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformLivingEntityRenderer(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("extractRenderState") && mn.desc.contains("F)V")) {
            for (AbstractInsnNode ret : findReturns(mn)) {
               InsnList p = new InsnList();
               p.add(new VarInsnNode(25, 1));
               p.add(new VarInsnNode(25, 2));
               p.add(new VarInsnNode(23, 3));
               p.add(new MethodInsnNode(184, "need/clarity/bridge/LivingEntityRendererBridge", "onExtractRenderStateTail", "(Ljava/lang/Object;Ljava/lang/Object;F)V", false));
               mn.instructions.insertBefore(ret, p);
            }
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("getRenderType")) {
            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new VarInsnNode(25, 0));
            p.add(new VarInsnNode(25, 1));
            p.add(new VarInsnNode(21, 2));
            p.add(new VarInsnNode(21, 3));
            p.add(new VarInsnNode(21, 4));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/LivingEntityRendererBridge", "overrideRenderType", "(Ljava/lang/Object;Ljava/lang/Object;ZZZ)Ljava/lang/Object;", false));
            p.add(new InsnNode(89));
            p.add(new JumpInsnNode(198, cont));
            p.add(new TypeInsnNode(192, "net/minecraft/client/renderer/rendertype/RenderType"));
            p.add(new InsnNode(176));
            p.add(cont);
            p.add(new InsnNode(87));
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("getModelTint")) {
            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new VarInsnNode(25, 1));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/LivingEntityRendererBridge", "overrideModelTint", "(Ljava/lang/Object;)I", false));
            p.add(new InsnNode(89));
            p.add(new LdcInsnNode(Integer.MIN_VALUE));
            p.add(new JumpInsnNode(159, cont));
            p.add(new InsnNode(172));
            p.add(cont);
            p.add(new InsnNode(87));
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("submit")) {
            InsnList p = new InsnList();
            p.add(new VarInsnNode(25, 0));
            p.add(new VarInsnNode(25, 3));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/LivingEntityRendererBridge", "wrapCollector", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false));
            p.add(new TypeInsnNode(192, "net/minecraft/client/renderer/SubmitNodeCollector"));
            p.add(new VarInsnNode(58, 3));
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
         }
      }

      return write(cn, cl);
   }

   private byte[] transformClientCommonPacketListener(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("send") && mn.desc.startsWith("(Lnet/minecraft/network/protocol/Packet;")) {
            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new VarInsnNode(25, 1));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/PacketListenerBridge", "onPacketSend", "(Lnet/minecraft/network/protocol/Packet;)Z", false));
            p.add(new JumpInsnNode(153, cont));
            p.add(new InsnNode(177));
            p.add(cont);
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);

            for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
               if (insn instanceof MethodInsnNode min && min.owner.equals("net/minecraft/network/Connection") && min.name.equals("send")) {
                  InsnList ah = new InsnList();
                  ah.add(
                     new MethodInsnNode(184, "need/clarity/bridge/PacketListenerBridge", "antiHungerModify", "(Lnet/minecraft/network/protocol/Packet;)Lnet/minecraft/network/protocol/Packet;", false)
                  );
                  mn.instructions.insertBefore(insn, ah);
                  return write(cn, cl);
               }
            }
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformClientBrandRetriever(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("getClientModName")) {
            for (AbstractInsnNode ret : findReturns(mn)) {
               if (ret.getOpcode() == 176) {
                  InsnList p = new InsnList();
                  p.add(new MethodInsnNode(184, "need/clarity/bridge/MiscBridge", "getClientBrand", "(Ljava/lang/String;)Ljava/lang/String;", false));
                  mn.instructions.insertBefore(ret, p);
               }
            }
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformCompiledSectionMesh(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("facesCanSeeEachother")) {
            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new MethodInsnNode(184, "need/clarity/bridge/MiscBridge", "shouldBypassOcclusionCulling", "()Z", false));
            p.add(new JumpInsnNode(153, cont));
            p.add(new InsnNode(4));
            p.add(new InsnNode(172));
            p.add(cont);
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformSlimeBlock(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("stepOn") && mn.desc.endsWith("Lnet/minecraft/world/entity/Entity;)V")) {
            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new VarInsnNode(25, 4));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/GameplayBridge", "noSlowSlime", "(Lnet/minecraft/world/entity/Entity;)Z", false));
            p.add(new JumpInsnNode(153, cont));
            p.add(new InsnNode(177));
            p.add(cont);
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformChatLineVisitor(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("accept") && mn.desc.equals("(Lnet/minecraft/client/multiplayer/chat/GuiMessage$Line;IF)V")) {
            InsnList p = new InsnList();
            p.add(new VarInsnNode(25, 1));
            p.add(new VarInsnNode(23, 3));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/ChatBridge", "lineStart", "(Ljava/lang/Object;F)V", false));
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);

            for (AbstractInsnNode ret : findReturns(mn)) {
               mn.instructions.insertBefore(ret, new MethodInsnNode(184, "need/clarity/bridge/ChatBridge", "lineEnd", "()V", false));
            }
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformStringDecomposer(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("iterateFormatted")
            && (mn.access & 8) != 0
            && mn.desc.equals("(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z")) {
            InsnList p = new InsnList();
            p.add(new VarInsnNode(25, 0));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/ChatBridge", "nameProtectText", "(Ljava/lang/String;)Ljava/lang/String;", false));
            p.add(new VarInsnNode(58, 0));
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformHumanoidModel(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("setupAnim") && mn.desc.equals("(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V")) {
            for (AbstractInsnNode ret : findReturns(mn)) {
               InsnList p = new InsnList();
               p.add(new VarInsnNode(25, 0));
               p.add(new VarInsnNode(25, 1));
               p.add(new MethodInsnNode(184, "need/clarity/bridge/MiscBridge", "moveAnimations", "(Ljava/lang/Object;Ljava/lang/Object;)V", false));
               mn.instructions.insertBefore(ret, p);
            }
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformPowderSnowBlock(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("canEntityWalkOnPowderSnow")) {
            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new VarInsnNode(25, 0));
            p.add(new InsnNode(3));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/MiscBridge", "canWalkOnPowderSnow", "(Ljava/lang/Object;Z)Z", false));
            p.add(new JumpInsnNode(153, cont));
            p.add(new InsnNode(4));
            p.add(new InsnNode(172));
            p.add(cont);
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformKeyboardHandler(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("charTyped") && mn.desc.equals("(JLnet/minecraft/client/input/CharacterEvent;)V")) {
            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new VarInsnNode(25, 3));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/KeyboardHandlerBridge", "onCharTyped", "(Lnet/minecraft/client/input/CharacterEvent;)Z", false));
            p.add(new JumpInsnNode(153, cont));
            p.add(new InsnNode(177));
            p.add(cont);
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("keyPress")) {
            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new VarInsnNode(22, 1));
            p.add(new VarInsnNode(21, 3));
            p.add(new VarInsnNode(25, 4));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/KeyboardHandlerBridge", "onKeyPress", "(JILnet/minecraft/client/input/KeyEvent;)Z", false));
            p.add(new JumpInsnNode(153, cont));
            p.add(new InsnNode(177));
            p.add(cont);
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformMouseHandler(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("turnPlayer")) {
            for (AbstractInsnNode insn : mn.instructions.toArray()) {
               if (insn instanceof MethodInsnNode min && min.name.equals("turn") && min.desc.equals("(DD)V") && min.owner.equals("net/minecraft/client/player/LocalPlayer")) {
                  mn.instructions.set(insn, new MethodInsnNode(184, "need/clarity/bridge/MouseHandlerBridge", "turnPlayer", "(Lnet/minecraft/client/player/LocalPlayer;DD)V", false));
               }
            }
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("onButton")) {
            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new VarInsnNode(22, 1));
            p.add(new VarInsnNode(25, 3));
            p.add(new VarInsnNode(21, 4));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/MouseHandlerBridge", "onMouseButton", "(JLnet/minecraft/client/input/MouseButtonInfo;I)Z", false));
            p.add(new JumpInsnNode(153, cont));
            p.add(new InsnNode(177));
            p.add(cont);
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("onScroll")) {
            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new VarInsnNode(22, 1));
            p.add(new VarInsnNode(24, 3));
            p.add(new VarInsnNode(24, 5));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/MouseHandlerBridge", "onScroll", "(JDD)Z", false));
            p.add(new JumpInsnNode(153, cont));
            p.add(new InsnNode(177));
            p.add(cont);
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformPlayerInfo(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("getSkin")) {
            for (AbstractInsnNode ret : findReturns(mn)) {
               if (ret.getOpcode() == 176) {
                  InsnList p = new InsnList();
                  p.add(new VarInsnNode(25, 0));
                  p.add(new InsnNode(95));
                  p.add(new MethodInsnNode(184, "need/clarity/bridge/MiscBridge", "overrideSkin", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false));
                  p.add(new TypeInsnNode(192, "net/minecraft/world/entity/player/PlayerSkin"));
                  mn.instructions.insertBefore(ret, p);
               }
            }
            break;
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("getTabListDisplayName")) {
            for (AbstractInsnNode ret : findReturns(mn)) {
               if (ret.getOpcode() == 176) {
                  InsnList p = new InsnList();
                  p.add(new VarInsnNode(25, 0));
                  p.add(new InsnNode(95));
                  p.add(new MethodInsnNode(184, "need/clarity/bridge/MiscBridge", "nameProtectTabList", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false));
                  p.add(new TypeInsnNode(192, "net/minecraft/network/chat/Component"));
                  p.add(new VarInsnNode(25, 0));
                  p.add(
                     new MethodInsnNode(
                        184,
                        "need/clarity/bridge/ChatBridge",
                        "friendTabList",
                        "(Lnet/minecraft/network/chat/Component;Lnet/minecraft/client/multiplayer/PlayerInfo;)Lnet/minecraft/network/chat/Component;",
                        false
                     )
                  );
                  mn.instructions.insertBefore(ret, p);
               }
            }
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformChatComponent(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);
      String CHAT = "net/minecraft/client/gui/components/ChatComponent";
      String LIST = "Ljava/util/List;";

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("addMessage")) {
            Type[] args = Type.getArgumentTypes(mn.desc);
            int idx = -1;

            for (int i = 0; i < args.length; i++) {
               if (args[i].getInternalName().equals("net/minecraft/network/chat/Component")) {
                  idx = i;
                  break;
               }
            }

            if (idx >= 0) {
               int slot = argSlot(mn, idx);
               InsnList p = new InsnList();
               p.add(new VarInsnNode(25, slot));
               p.add(new VarInsnNode(25, 0));
               p.add(new FieldInsnNode(180, cn.name, "allMessages", "Ljava/util/List;"));
               p.add(new VarInsnNode(25, 0));
               p.add(new FieldInsnNode(180, cn.name, "trimmedMessages", "Ljava/util/List;"));
               p.add(
                  new MethodInsnNode(
                     184, "need/clarity/bridge/ChatBridge", "onAdd", "(Lnet/minecraft/network/chat/Component;Ljava/util/List;Ljava/util/List;)Lnet/minecraft/network/chat/Component;", false
                  )
               );
               p.add(new VarInsnNode(58, slot));
               mn.instructions.insertBefore(mn.instructions.getFirst(), p);

               for (AbstractInsnNode ret : findReturns(mn)) {
                  InsnList t = new InsnList();
                  t.add(new VarInsnNode(25, 0));
                  t.add(new FieldInsnNode(180, cn.name, "allMessages", "Ljava/util/List;"));
                  t.add(new MethodInsnNode(184, "need/clarity/bridge/ChatBridge", "afterAdd", "(Ljava/util/List;)V", false));
                  mn.instructions.insertBefore(ret, t);
               }
            }
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("addMessageToQueue") || mn.name.equals("addMessageToDisplayQueue")) {
            for (AbstractInsnNode insn : mn.instructions.toArray()) {
               if (insn.getOpcode() == 16 && ((IntInsnNode)insn).operand == 100) {
                  mn.instructions.insert(insn, new MethodInsnNode(184, "need/clarity/bridge/ChatBridge", "historyLimit", "(I)I", false));
               }
            }
         }

         if (mn.name.equals("extractRenderState") && mn.desc.startsWith("(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;")) {
            InsnList p = new InsnList();
            p.add(new VarInsnNode(25, 1));
            p.add(
               new MethodInsnNode(
                  184,
                  "need/clarity/bridge/ChatBridge",
                  "decorate",
                  "(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;)Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;",
                  false
               )
            );
            p.add(new VarInsnNode(58, 1));
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
         }

         if (mn.name.equals("extractRenderState") && mn.desc.startsWith("(Lnet/minecraft/client/gui/GuiGraphicsExtractor;") || mn.name.equals("captureClickableText")) {
            insertCancellableHead(mn, "need/clarity/bridge/ChatBridge", "hideVanillaChat", "()Z");
         }

         if (mn.name.equals("lambda$extractRenderState$1") && (mn.access & 8) != 0) {
            Type[] a = Type.getArgumentTypes(mn.desc);
            int lineIdx = -1;
            int alphaIdx = -1;

            for (int i = 0; i < a.length; i++) {
               if (a[i].getSort() == 10 && a[i].getInternalName().equals("net/minecraft/client/multiplayer/chat/GuiMessage$Line")) {
                  lineIdx = i;
               }

               if (a[i].getSort() == 6) {
                  alphaIdx = i;
               }
            }

            if (lineIdx >= 0 && alphaIdx >= 0) {
               InsnList p = new InsnList();
               p.add(new VarInsnNode(25, argSlot(mn, lineIdx)));
               p.add(new VarInsnNode(23, argSlot(mn, alphaIdx)));
               p.add(new MethodInsnNode(184, "need/clarity/bridge/ChatBridge", "lineStart", "(Ljava/lang/Object;F)V", false));
               mn.instructions.insertBefore(mn.instructions.getFirst(), p);

               for (AbstractInsnNode ret : findReturns(mn)) {
                  mn.instructions.insertBefore(ret, new MethodInsnNode(184, "need/clarity/bridge/ChatBridge", "lineEnd", "()V", false));
               }
            }
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("addMessage")) {
            InsnList p = new InsnList();
            p.add(new VarInsnNode(25, 1));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/MiscBridge", "nameProtectChat", "(Ljava/lang/Object;)Ljava/lang/Object;", false));
            p.add(new TypeInsnNode(192, "net/minecraft/network/chat/Component"));
            p.add(new VarInsnNode(58, 1));
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformChatScreen(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);
      MethodNode insMode = null;
      MethodNode handleClick = null;

      for (MethodNode m : cn.methods) {
         if (m.name.equals("insertionClickMode") && m.desc.equals("()Z")) {
            insMode = m;
         }

         if (m.name.equals("handleComponentClicked") && m.desc.equals("(Lnet/minecraft/network/chat/Style;Z)Z")) {
            handleClick = m;
         }
      }

      if (insMode != null && handleClick != null) {
         for (MethodNode mn : cn.methods) {
            if (mn.name.equals("mouseClicked") && mn.desc.equals("(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z")) {
               int tmp = mn.maxLocals++;
               InsnList p = new InsnList();
               LabelNode cont = new LabelNode();
               p.add(new VarInsnNode(25, 1));
               p.add(new MethodInsnNode(184, "need/clarity/bridge/ChatBridge", "clickedStyle", "(Lnet/minecraft/client/input/MouseButtonEvent;)Lnet/minecraft/network/chat/Style;", false));
               p.add(new VarInsnNode(58, tmp));
               p.add(new VarInsnNode(25, tmp));
               p.add(new JumpInsnNode(198, cont));
               p.add(new VarInsnNode(25, 0));
               p.add(new VarInsnNode(25, tmp));
               p.add(new VarInsnNode(25, 0));
               p.add(new MethodInsnNode(182, cn.name, "insertionClickMode", "()Z", false));
               p.add(new MethodInsnNode(182, cn.name, "handleComponentClicked", "(Lnet/minecraft/network/chat/Style;Z)Z", false));
               p.add(new JumpInsnNode(153, cont));
               p.add(new InsnNode(4));
               p.add(new InsnNode(172));
               p.add(cont);
               mn.instructions.insertBefore(mn.instructions.getFirst(), p);
               break;
            }
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("handleChatInput") && mn.desc.equals("(Ljava/lang/String;Z)V")) {
            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new VarInsnNode(25, 1));
            p.add(new VarInsnNode(21, 2));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/ChatBridge", "interceptInput", "(Ljava/lang/String;Z)Z", false));
            p.add(new JumpInsnNode(153, cont));
            p.add(new InsnNode(177));
            p.add(cont);
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("handleChatInput")) {
            InsnList p = new InsnList();
            p.add(new VarInsnNode(25, 1));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/MiscBridge", "nameProtectChatInput", "(Ljava/lang/String;)Ljava/lang/String;", false));
            p.add(new VarInsnNode(58, 1));
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformPlayerTabOverlay(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (String mName : new String[]{"render", "renderTablistScore"}) {
         for (MethodNode mn : cn.methods) {
            if (mn.name.equals(mName)) {
               break;
            }
         }
      }

      return write(cn, cl);
   }

   private byte[] transformItemInHandRenderer(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("renderArmWithItem")) {
            InsnList headP = new InsnList();
            headP.add(new VarInsnNode(25, 1));
            headP.add(new VarInsnNode(25, 4));
            headP.add(new VarInsnNode(23, 7));
            headP.add(new VarInsnNode(25, 8));
            headP.add(
               new MethodInsnNode(
                  184, "need/clarity/bridge/ViewModelBridge", "onRenderArmWithItemHead", "(Ljava/lang/Object;Lnet/minecraft/world/InteractionHand;FLcom/mojang/blaze3d/vertex/PoseStack;)Z", false
               )
            );
            LabelNode vmCont = new LabelNode();
            headP.add(new JumpInsnNode(153, vmCont));
            headP.add(new InsnNode(177));
            headP.add(vmCont);
            mn.instructions.insertBefore(mn.instructions.getFirst(), headP);

            for (AbstractInsnNode ret : findReturns(mn)) {
               InsnList retP = new InsnList();
               retP.add(new VarInsnNode(25, 8));
               retP.add(new MethodInsnNode(184, "need/clarity/bridge/ViewModelBridge", "onRenderArmWithItemReturn", "(Lcom/mojang/blaze3d/vertex/PoseStack;)V", false));
               mn.instructions.insertBefore(ret, retP);
            }

            for (AbstractInsnNode insn : mn.instructions.toArray()) {
               if (insn instanceof MethodInsnNode min
                  && min.name.equals("renderItem")
                  && min.owner.equals("net/minecraft/client/renderer/ItemInHandRenderer")
                  && min.desc
                     .equals(
                        "(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"
                     )) {
                  InsnList hv = new InsnList();
                  hv.add(new VarInsnNode(25, 1));
                  hv.add(new VarInsnNode(25, 4));
                  hv.add(new VarInsnNode(23, 5));
                  hv.add(new VarInsnNode(23, 7));
                  hv.add(
                     new MethodInsnNode(
                        184,
                        "need/clarity/bridge/ViewModelBridge",
                        "handViewRenderItem",
                        "(Lnet/minecraft/client/renderer/ItemInHandRenderer;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/world/InteractionHand;FF)V",
                        false
                     )
                  );
                  mn.instructions.insertBefore(insn, hv);
                  mn.instructions.remove(insn);
               }
            }

            int applyOrdinal = 0;

            for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
               if (insn instanceof MethodInsnNode min && min.name.equals("applyItemArmTransform")) {
                  if (++applyOrdinal == 5) {
                     InsnList p = new InsnList();
                     new LabelNode();
                     new LabelNode();
                     p.add(new InsnNode(92));
                     break;
                  }
               }
            }

            int swingOrdinal = 0;

            for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
               if (insn instanceof MethodInsnNode min && min.name.equals("swingArm")) {
                  if (++swingOrdinal == 3) {
                     InsnList p = new InsnList();
                     p.add(new VarInsnNode(25, 8));
                     p.add(new MethodInsnNode(184, "need/clarity/bridge/MiscBridge", "animationsApplyAfterSwing", "(Lcom/mojang/blaze3d/vertex/PoseStack;)V", false));
                     mn.instructions.insert(insn, p);
                     return write(cn, cl);
                  }
               }
            }
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformEndCrystalRenderer(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("submit")) {
            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new VarInsnNode(25, 1));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/MiscBridge", "shouldCancelCrystalRender", "(Ljava/lang/Object;)Z", false));
            p.add(new JumpInsnNode(153, cont));
            p.add(new InsnNode(177));
            p.add(cont);
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] wrapReloadResourceManager(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);
      String rmDesc = "Lnet/minecraft/server/packs/resources/ResourceManager;";

      for (MethodNode mn : cn.methods) {
         if ((mn.name.equals("prepare") || mn.name.equals("apply")) && (mn.access & 5440) == 0) {
            Type[] args = Type.getArgumentTypes(mn.desc);
            int slot = (mn.access & 8) != 0 ? 0 : 1;

            for (Type a : args) {
               if (a.getDescriptor().equals("Lnet/minecraft/server/packs/resources/ResourceManager;")) {
                  InsnList p = new InsnList();
                  p.add(new VarInsnNode(25, slot));
                  p.add(new MethodInsnNode(184, "need/clarity/bridge/MiscBridge", "wrapResourceManager", "(Ljava/lang/Object;)Ljava/lang/Object;", false));
                  p.add(new TypeInsnNode(192, "net/minecraft/server/packs/resources/ResourceManager"));
                  p.add(new VarInsnNode(58, slot));
                  mn.instructions.insertBefore(mn.instructions.getFirst(), p);
                  break;
               }

               slot += a.getSize();
            }
         }
      }

      return write(cn, cl);
   }

   private byte[] transformSoundManager(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("play")) {
            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new VarInsnNode(25, 1));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/MiscBridge", "shouldMuteCrystalSound", "(Ljava/lang/Object;)Z", false));
            p.add(new JumpInsnNode(153, cont));
            p.add(new FieldInsnNode(178, "net/minecraft/client/sounds/SoundEngine$PlayResult", "NOT_STARTED", "Lnet/minecraft/client/sounds/SoundEngine$PlayResult;"));
            p.add(new InsnNode(176));
            p.add(cont);
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformCapeLayer(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("submit")) {
            for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
               if (insn instanceof FieldInsnNode fin && fin.getOpcode() == 180 && fin.name.equals("isInvisible")) {
                  new InsnList();
                  InsnList dup = new InsnList();
                  dup.add(new InsnNode(89));
                  mn.instructions.insertBefore(insn, dup);
                  InsnList wrap = new InsnList();
                  wrap.add(new InsnNode(95));
                  wrap.add(new MethodInsnNode(184, "need/clarity/bridge/MiscBridge", "capeLayerIsInvisible", "(ZLjava/lang/Object;)Z", false));
                  mn.instructions.insert(insn, wrap);
                  return write(cn, cl);
               }
            }
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformItemInHandLayer(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("submit")) {
            InsnList head = new InsnList();
            head.add(new VarInsnNode(25, 0));
            head.add(new VarInsnNode(25, 4));
            head.add(new MethodInsnNode(184, "need/clarity/bridge/MiscBridge", "modelHandsBegin", "(Ljava/lang/Object;Ljava/lang/Object;)V", false));
            mn.instructions.insertBefore(mn.instructions.getFirst(), head);

            for (AbstractInsnNode ret : findReturns(mn)) {
               InsnList tail = new InsnList();
               tail.add(new MethodInsnNode(184, "need/clarity/bridge/MiscBridge", "modelHandsEnd", "()V", false));
               mn.instructions.insertBefore(ret, tail);
            }
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformModelPart(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("translateAndRotate")) {
            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new VarInsnNode(25, 0));
            p.add(new VarInsnNode(25, 1));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/MiscBridge", "modelPartTranslateAndRotate", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false));
            p.add(new JumpInsnNode(153, cont));
            p.add(new InsnNode(177));
            p.add(cont);
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformCrashReportCategory(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("validateStackTrace")) {
            for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
               if (insn instanceof MethodInsnNode min && min.name.equals("getFileName")) {
                  InsnList wrap = new InsnList();
                  wrap.add(new MethodInsnNode(184, "need/clarity/bridge/MiscBridge", "safeFileName", "(Ljava/lang/String;)Ljava/lang/String;", false));
                  mn.instructions.insert(insn, wrap);
                  return write(cn, cl);
               }
            }
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformPostChain(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("load") && (mn.access & 8) != 0) {
            for (AbstractInsnNode ret : findReturns(mn)) {
               if (ret.getOpcode() == 176) {
                  InsnList p = new InsnList();
                  p.add(new InsnNode(89));
                  p.add(new VarInsnNode(25, 3));
                  p.add(new MethodInsnNode(184, "need/clarity/bridge/MiscBridge", "onPostChainLoaded", "(Ljava/lang/Object;Ljava/lang/Object;)V", false));
                  mn.instructions.insertBefore(ret, p);
               }
            }
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformPostPass(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.startsWith("lambda$addToFrame")) {
            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new VarInsnNode(25, 0));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/MiscBridge", "onPostPassLambda", "(Ljava/lang/Object;)Z", false));
            p.add(new JumpInsnNode(153, cont));
            p.add(new InsnNode(177));
            p.add(cont);
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      return write(cn, cl);
   }

   private byte[] transformProjectionMatrixBuffer(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("getBuffer")) {
            InsnList p = new InsnList();
            p.add(new VarInsnNode(25, 0));
            p.add(new VarInsnNode(25, 1));
            p.add(new MethodInsnNode(184, "need/clarity/bridge/MiscBridge", "onProjectionGetBuffer", "(Ljava/lang/Object;Ljava/lang/Object;)V", false));
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
         }
      }

      return write(cn, cl);
   }

   private byte[] transformClientPacketListener(byte[] bytes, ClassLoader cl) {
      ClassNode cn = read(bytes);

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("handleContainerClose")) {
            for (AbstractInsnNode insn : mn.instructions.toArray()) {
               if (insn instanceof MethodInsnNode min && min.name.equals("clientSideCloseContainer") && min.desc.equals("()V")) {
                  mn.instructions.set(insn, new MethodInsnNode(184, "need/clarity/bridge/GameplayBridge", "serverClose", "(Lnet/minecraft/client/player/LocalPlayer;)V", false));
               }
            }
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("handleOpenScreen")) {
            insertHeadCall(mn, "need/clarity/bridge/GameplayBridge", "openScreenStart", "()V");

            for (AbstractInsnNode ret : findReturns(mn)) {
               mn.instructions.insertBefore(ret, new MethodInsnNode(184, "need/clarity/bridge/GameplayBridge", "openScreenEnd", "()V", false));
            }
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("handleAddEntity")) {
            for (AbstractInsnNode ret : findReturns(mn)) {
               InsnList p = new InsnList();
               p.add(new VarInsnNode(25, 1));
               p.add(new MethodInsnNode(184, "need/clarity/bridge/GameplayBridge", "onEntityAdded", "(Lnet/minecraft/network/protocol/game/ClientboundAddEntityPacket;)V", false));
               mn.instructions.insertBefore(ret, p);
            }
         }
      }

      for (MethodNode mn : cn.methods) {
         if (mn.name.equals("handleExplosion")) {
            for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
               if (insn instanceof MethodInsnNode min && min.name.equals("addParticle") && min.owner.contains("ClientLevel")) {
                  break;
               }
            }

            InsnList p = new InsnList();
            LabelNode cont = new LabelNode();
            p.add(new MethodInsnNode(184, "need/clarity/bridge/MiscBridge", "shouldCancelAllExplosionParticles", "()Z", false));
            p.add(new JumpInsnNode(153, cont));
            p.add(new InsnNode(177));
            p.add(cont);
            mn.instructions.insertBefore(mn.instructions.getFirst(), p);
            break;
         }
      }

      return write(cn, cl);
   }
}
