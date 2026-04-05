package dev.stacksizeconf.client;

import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import dev.stacksizeconf.StackSizeConfig;
import dev.stacksizeconf.StackSizeMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class LightLevelOverlay {
    private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(StackSizeMod.MOD_ID, "tools")
    );
    private static final KeyMapping TOGGLE = new KeyMapping(
            "key.stacksizeconf.light_level_overlay",
            GLFW.GLFW_KEY_L,
            KEY_CATEGORY
    );

    /** Shown in-world while {@link StackSizeConfig#ENABLE_LIGHT_LEVEL_OVERLAY} is true; toggled with the hotkey. */
    private static boolean visible;
    private static boolean lastOverlayConfigEnabled;

    private LightLevelOverlay() {
    }

    public static KeyMapping toggleKey() {
        return TOGGLE;
    }

    /** Default key + modifier; used by config "reset module" for the hotkey row. */
    public static void resetHotkeyToDefault() {
        TOGGLE.setToDefault();
        TOGGLE.setKey(TOGGLE.getDefaultKey());
        KeyMapping.resetMapping();
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(LightLevelOverlay::onRegisterKeys);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(LightLevelOverlay::onClientTick);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(LightLevelOverlay::onRenderAfterEntities);
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        boolean enabled = StackSizeConfig.ENABLE_LIGHT_LEVEL_OVERLAY.get();
        if (!enabled) {
            visible = false;
            lastOverlayConfigEnabled = false;
            return;
        }
        if (!lastOverlayConfigEnabled) {
            visible = true;
        }
        lastOverlayConfigEnabled = true;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }
        while (TOGGLE.consumeClick()) {
            visible = !visible;
        }
    }

    private static void onRenderAfterEntities(RenderLevelStageEvent.AfterEntities event) {
        if (!visible || !StackSizeConfig.ENABLE_LIGHT_LEVEL_OVERLAY.get()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.font == null) {
            return;
        }
        LevelRenderState levelRenderState = event.getLevelRenderState();
        CameraRenderState camState = levelRenderState.cameraRenderState;
        if (!camState.initialized) {
            return;
        }
        Level level = mc.level;
        int hr = StackSizeConfig.LIGHT_OVERLAY_HORIZONTAL_RANGE.get();
        int vr = StackSizeConfig.LIGHT_OVERLAY_VERTICAL_RANGE.get();
        BlockPos origin = mc.player.blockPosition();
        int px = origin.getX();
        int py = origin.getY();
        int pz = origin.getZ();
        int minY = Mth.clamp(py - vr, level.getMinY(), level.getMaxY());
        int maxY = Mth.clamp(py + vr, level.getMinY(), level.getMaxY());

        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        Vec3 eye = mc.player.getEyePosition(partialTick);

        PoseStack poseStack = new PoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        Font font = mc.font;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -hr; dx <= hr; dx++) {
            for (int dz = -hr; dz <= hr; dz++) {
                for (int y = minY; y <= maxY; y++) {
                    pos.set(px + dx, y, pz + dz);
                    if (!isOverlayAnchor(level, pos)) {
                        continue;
                    }
                    double lx = pos.getX() + 0.5;
                    double ly = pos.getY() + 1.0 + 0.12;
                    double lz = pos.getZ() + 0.5;
                    Vec3 labelPos = new Vec3(lx, ly, lz);
                    if (!isLabelUnobstructed(level, mc.player, eye, labelPos, pos.immutable())) {
                        continue;
                    }
                    BlockPos above = pos.above();
                    int light = switch (StackSizeConfig.LIGHT_OVERLAY_BRIGHTNESS_MODE.get()) {
                        case BLOCK -> level.getBrightness(LightLayer.BLOCK, above);
                        case COMBINED -> level.getRawBrightness(above, level.getSkyDarken());
                    };
                    String text = Integer.toString(light);
                    int color = light == 0 ? 0xFFFF0000 : 0xFF00FF00;
                    drawHorizontalTopLabel(font, buffers, poseStack, camState.pos, lx, ly, lz, text, color);
                }
            }
        }
    }

    /**
     * The label sits slightly above the floor block's top face; a ray eye→label hits that top face first,
     * so it is closer than the label and must not be treated as occlusion. Accept hits on {@code floorPos}.
     */
    private static boolean isLabelUnobstructed(Level level, Player player, Vec3 eye, Vec3 labelPos, BlockPos floorPos) {
        HitResult hit = level.clip(new ClipContext(eye, labelPos, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) {
            return true;
        }
        if (hit instanceof BlockHitResult bhr) {
            if (bhr.getBlockPos().equals(floorPos)) {
                return true;
            }
        }
        double distHit = Math.sqrt(eye.distanceToSqr(hit.getLocation()));
        double distTarget = Math.sqrt(eye.distanceToSqr(labelPos));
        return distHit >= distTarget - 0.45;
    }

    private static boolean isOverlayAnchor(Level level, BlockPos pos) {
        BlockState floor = level.getBlockState(pos);
        if (!floor.isFaceSturdy(level, pos, Direction.UP)) {
            return false;
        }
        BlockState above = level.getBlockState(pos.above());
        if (!above.getFluidState().isEmpty()) {
            return false;
        }
        return !above.blocksMotion();
    }

    /**
     * Lays text in the horizontal plane (parallel to the block top), not camera-facing.
     * {@link Font.DisplayMode#POLYGON_OFFSET} keeps quads from z-fighting with the surface below.
     */
    private static void drawHorizontalTopLabel(
            Font font,
            MultiBufferSource.BufferSource buffers,
            PoseStack poseStack,
            Vec3 camPos,
            double wx,
            double wy,
            double wz,
            String text,
            int color
    ) {
        float x = (float) (wx - camPos.x);
        float y = (float) (wy - camPos.y);
        float z = (float) (wz - camPos.z);
        float textWidth = font.width(text);
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        // Glyphs face +local Z; +90° X maps that to -world Y (backface when looking down). Use -90° X so normals are +Y.
        poseStack.mulPose(Axis.XN.rotationDegrees(90.0F));
        poseStack.scale(0.038F, -0.038F, 0.038F);
        Matrix4f matrix = new Matrix4f(poseStack.last().pose());
        font.drawInBatch(
                text,
                -textWidth / 2.0F,
                0.0F,
                color,
                false,
                matrix,
                buffers,
                Font.DisplayMode.POLYGON_OFFSET,
                0,
                LightTexture.FULL_BRIGHT
        );
        poseStack.popPose();
    }
}
