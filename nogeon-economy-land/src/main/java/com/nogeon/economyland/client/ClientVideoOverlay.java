package com.nogeon.economyland.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.imageio.ImageIO;

public final class ClientVideoOverlay {
    private static final String RESOURCE_PATH = "/assets/nogeon_economy_land/videos/OpenEye.mp4";
    private static final String FRAME_RESOURCE_PATTERN = "/assets/nogeon_economy_land/openeye_frames/OpenEye%02d.gif";
    private static final ResourceLocation OPEN_EYE_SOUND = new ResourceLocation("nogeon_economy_land", "open_eye");
    private static final int FRAME_COUNT = 57;
    private static final long FRAME_DURATION_MS = 55L;
    private static File tempVideoFile = null;
    private static final Queue<BufferedImage> frameQueue = new ConcurrentLinkedQueue<>();
    private static final List<BufferedImage> frameSequence = new ArrayList<>();
    private static DynamicTexture dynamicTexture = null;
    private static ResourceLocation textureLocation = null;
    private static boolean isPlaying = false;
    private static float alpha = 0.0F;
    private static long startTime = 0L;
    private static Thread decodingThread = null;
    private static final int TARGET_WIDTH = 640;
    private static final int TARGET_HEIGHT = 360;
    private static Boolean decoderAvailable = null;
    private static boolean fallbackMode = false;
    private static boolean sequenceMode = false;
    private static int lastSequenceFrame = -1;

    private ClientVideoOverlay() {
    }

    private static File getOrCreateTempVideoFile() {
        if (tempVideoFile != null && tempVideoFile.exists() && tempVideoFile.length() > 0) {
            return tempVideoFile;
        }

        try {
            File tempDir = new File(System.getProperty("java.io.tmpdir"));
            File file = new File(tempDir, "nogeon_economy_land_OpenEye.mp4");
            
            java.io.InputStream in = ClientVideoOverlay.class.getResourceAsStream(RESOURCE_PATH);
            if (in == null) {
                return null;
            }

            try (in; java.io.FileOutputStream out = new java.io.FileOutputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            
            tempVideoFile = file;
            return tempVideoFile;
        } catch (Exception e) {
            return null;
        }
    }

    public static void start() {
        stop();
        isPlaying = true;
        alpha = 0.5F;
        startTime = System.currentTimeMillis();
        fallbackMode = false;
        playOpenEyeSound();

        if (loadFrameSequence()) {
            fallbackMode = false;
            sequenceMode = true;
            return;
        }

        if (!isDecoderAvailable()) {
            isPlaying = false;
            return;
        }

        File videoFile = getOrCreateTempVideoFile();
        if (videoFile == null || !videoFile.exists()) {
            isPlaying = false;
            return;
        }

        // 기존 재생 중인 스레드가 있으면 정리
        stop();

        isPlaying = true;
        alpha = 0.5F; // 50% 투명도로 시작
        startTime = System.currentTimeMillis();

        fallbackMode = false;
        decodingThread = new Thread(() -> {
            try {
                Class<?> frameGrabClass = Class.forName("org.jcodec.api.FrameGrab");
                Class<?> nioUtilsClass = Class.forName("org.jcodec.common.io.NIOUtils");
                Class<?> channelClass = Class.forName("org.jcodec.common.io.SeekableByteChannel");
                Class<?> pictureClass = Class.forName("org.jcodec.common.model.Picture");
                Class<?> awtUtilClass = Class.forName("org.jcodec.scale.AWTUtil");
                Object channel = nioUtilsClass.getMethod("readableChannel", File.class).invoke(null, videoFile);
                Object grab = frameGrabClass.getMethod("createFrameGrab", channelClass).invoke(null, channel);
                Method getNativeFrame = frameGrabClass.getMethod("getNativeFrame");
                Method toBufferedImage = awtUtilClass.getMethod("toBufferedImage", pictureClass);
                Object picture;
                while (isPlaying && (picture = getNativeFrame.invoke(grab)) != null) {
                    BufferedImage frame = (BufferedImage) toBufferedImage.invoke(null, picture);
                    BufferedImage scaled = resizeImage(frame, TARGET_WIDTH, TARGET_HEIGHT);
                    frameQueue.add(scaled);

                    // 메모리 과다 사용 방지를 위해 큐 적체 시 대기
                    while (isPlaying && frameQueue.size() > 15) {
                        Thread.sleep(10);
                    }

                    // 디코딩 속도가 프레임 재생보다 너무 빠르지 않도록 조절
                    Thread.sleep(15);
                }
            } catch (Throwable e) {
                stop();
                // 비디오 예외 처리
            }
        });
        decodingThread.setName("OpenEye Video Decoder Thread");
        decodingThread.setDaemon(true);
        decodingThread.start();
    }

    private static void playOpenEyeSound() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getSoundManager() == null) {
            return;
        }
        SoundEvent sound = SoundEvent.createVariableRangeEvent(OPEN_EYE_SOUND);
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F, 0.85F));
    }

    public static void stop() {
        isPlaying = false;
        alpha = 0.0F;
        if (decodingThread != null) {
            decodingThread.interrupt();
            decodingThread = null;
        }
        frameQueue.clear();
        fallbackMode = false;
        sequenceMode = false;
        lastSequenceFrame = -1;
    }

    private static boolean loadFrameSequence() {
        if (!frameSequence.isEmpty()) {
            return true;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;

        // 다양한 프레임 명명 규칙 및 확장자 패턴을 지원하여 사용자가 어떻게 추가했든 100% 탐지하도록 조율
        String[] formats = new String[] {
            "openeye_frames/OpenEye%02d.gif",
            "openeye_frames/OpenEye%d.gif",
            "openeye_frames/OpenEye%02d.png",
            "openeye_frames/OpenEye%d.png",
            "openeye_frames/openeye%02d.gif",
            "openeye_frames/openeye%d.gif",
            "openeye_frames/openeye%02d.png",
            "openeye_frames/openeye%d.png",
            "openeye_frames/%02d.gif",
            "openeye_frames/%d.gif",
            "openeye_frames/%02d.png",
            "openeye_frames/%d.png"
        };

        for (String format : formats) {
            frameSequence.clear();
            boolean success = true;

            // 0 기반 스캔 시도 (0 ~ 56)
            for (int i = 0; i < FRAME_COUNT; i++) {
                ResourceLocation loc = new ResourceLocation("nogeon_economy_land", String.format(java.util.Locale.ROOT, format, i));
                try {
                    var resource = mc.getResourceManager().getResource(loc);
                    if (resource.isPresent()) {
                        try (java.io.InputStream in = resource.get().open()) {
                            BufferedImage frame = ImageIO.read(in);
                            if (frame != null) {
                                frameSequence.add(resizeImage(frame, TARGET_WIDTH, TARGET_HEIGHT));
                            } else {
                                success = false;
                                break;
                            }
                        }
                    } else {
                        success = false;
                        break;
                    }
                } catch (Exception e) {
                    success = false;
                    break;
                }
            }

            if (success && frameSequence.size() == FRAME_COUNT) {
                return true;
            }

            // 1 기반 스캔 시도 (1 ~ 57)
            frameSequence.clear();
            success = true;
            for (int i = 1; i <= FRAME_COUNT; i++) {
                ResourceLocation loc = new ResourceLocation("nogeon_economy_land", String.format(java.util.Locale.ROOT, format, i));
                try {
                    var resource = mc.getResourceManager().getResource(loc);
                    if (resource.isPresent()) {
                        try (java.io.InputStream in = resource.get().open()) {
                            BufferedImage frame = ImageIO.read(in);
                            if (frame != null) {
                                frameSequence.add(resizeImage(frame, TARGET_WIDTH, TARGET_HEIGHT));
                            } else {
                                success = false;
                                break;
                            }
                        }
                    } else {
                        success = false;
                        break;
                    }
                } catch (Exception e) {
                    success = false;
                    break;
                }
            }

            if (success && frameSequence.size() == FRAME_COUNT) {
                return true;
            }
        }

        frameSequence.clear();
        return false;
    }

    private static boolean isDecoderAvailable() {
        if (decoderAvailable != null) {
            return decoderAvailable.booleanValue();
        }
        try {
            Class.forName("org.jcodec.api.FrameGrab");
            Class.forName("org.jcodec.common.io.NIOUtils");
            Class.forName("org.jcodec.common.io.SeekableByteChannel");
            Class.forName("org.jcodec.common.model.Picture");
            Class.forName("org.jcodec.scale.AWTUtil");
            decoderAvailable = Boolean.TRUE;
        } catch (ReflectiveOperationException | LinkageError e) {
            decoderAvailable = Boolean.FALSE;
        }
        return decoderAvailable.booleanValue();
    }

    private static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        graphics2D.dispose();
        return resizedImage;
    }

    public static void render(GuiGraphics graphics, int width, int height) {
        if (!isPlaying) {
            return;
        }

        long elapsed = System.currentTimeMillis() - startTime;
        // 3.5초 (3500ms) 동안 비디오가 반투명 상태로 보여지며 서서히 페이드 아웃됨
        if (elapsed > 3500) {
            stop();
            return;
        }

        // 서서히 페이드 아웃 (0.5에서 시작해 0.0으로)
        alpha = 0.5F * (1.0F - (float) elapsed / 3500.0F);
        if (alpha < 0.0F) {
            alpha = 0.0F;
        }

        BufferedImage nextFrame = null;
        if (sequenceMode && !frameSequence.isEmpty()) {
            int frameIndex = (int) Math.min(frameSequence.size() - 1, elapsed / FRAME_DURATION_MS);
            if (frameIndex != lastSequenceFrame) {
                lastSequenceFrame = frameIndex;
                nextFrame = frameSequence.get(frameIndex);
            }
        } else {
            nextFrame = frameQueue.poll();
        }
        if (nextFrame != null) {
            try {
                if (dynamicTexture == null) {
                    dynamicTexture = new DynamicTexture(TARGET_WIDTH, TARGET_HEIGHT, false);
                    textureLocation = Minecraft.getInstance().getTextureManager().register("openeye_frame", dynamicTexture);
                }
                NativeImage nativeImage = dynamicTexture.getPixels();
                if (nativeImage != null) {
                    for (int y = 0; y < TARGET_HEIGHT; y++) {
                        for (int x = 0; x < TARGET_WIDTH; x++) {
                            int argb = nextFrame.getRGB(x, y);
                            int a = (argb >> 24) & 0xFF;
                            int r = (argb >> 16) & 0xFF;
                            int g = (argb >> 8) & 0xFF;
                            int b = argb & 0xFF;
                            int pixel = (a << 24) | (b << 16) | (g << 8) | r;
                            nativeImage.setPixelRGBA(x, y, pixel);
                        }
                    }
                    dynamicTexture.upload();
                }
            } catch (Exception e) {
                // 예외 처리
            }
        }

        if (textureLocation != null && alpha > 0.001F) {
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
            
            // 전체 화면 크기로 blit하여 렌더링
            graphics.blit(textureLocation, 0, 0, width, height, 0.0F, 0.0F, TARGET_WIDTH, TARGET_HEIGHT, TARGET_WIDTH, TARGET_HEIGHT);
            
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        }
    }

    private static void renderFallback(GuiGraphics graphics, int width, int height, long elapsed) {
        int overlayAlpha = Math.min(120, Math.max(0, (int) (alpha * 170.0F)));
        int blue = (overlayAlpha << 24) | 0x003078FF;
        int gold = (Math.min(170, overlayAlpha + 35) << 24) | 0x00FFD56A;
        graphics.fill(0, 0, width, height, blue);
        int scanY = (int) ((elapsed / 8L) % Math.max(1, height));
        for (int y = -height; y < height * 2; y += 24) {
            int lineY = y + scanY;
            graphics.fill(0, lineY, width, lineY + 2, gold);
        }
        int insetX = Math.max(12, width / 18);
        int insetY = Math.max(8, height / 16);
        int shade = overlayAlpha << 24;
        graphics.fill(0, 0, width, insetY, shade);
        graphics.fill(0, height - insetY, width, height, shade);
        graphics.fill(0, 0, insetX, height, shade);
        graphics.fill(width - insetX, 0, width, height, shade);
    }
}
