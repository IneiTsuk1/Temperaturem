package net.IneiTsuki.temperaturem.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class TemperatureHudRenderer implements HudRenderCallback {
    private static final Identifier TEXTURE =
            new Identifier("temperaturem", "textures/gui/temperature_indicator.png");

    // Animation state
    private float pulseAnimation = 0f;
    private static final float PULSE_SPEED = 0.05f;

    // Temperature thresholds
    private static final int COLD_THRESHOLD = 10;
    private static final int COMFORTABLE_LOW = 15;
    private static final int COMFORTABLE_HIGH = 30;
    private static final int HOT_THRESHOLD = 40;
    private static final int DANGER_COLD = -10;
    private static final int DANGER_HOT = 60;

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;

        // Update smooth temperature interpolation
        ClientPlayerTemperature.tick();

        int temp = ClientPlayerTemperature.get();
        float exactTemp = ClientPlayerTemperature.getExact();

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // Position: right side above hotbar (less intrusive than center)
        int x = screenWidth / 2 + 92; // Right of hotbar
        int y = screenHeight - 50;

        // Update pulse animation for extreme temperatures
        boolean isDangerous = temp <= DANGER_COLD || temp >= DANGER_HOT;
        if (isDangerous) {
            pulseAnimation += PULSE_SPEED;
            if (pulseAnimation > 1f) pulseAnimation = 0f;
        } else {
            pulseAnimation = 0f;
        }

        // Draw the base icon
        context.getMatrices().push();
        RenderSystem.enableBlend();

        // Apply pulsing effect for dangerous temperatures
        if (isDangerous) {
            float scale = 1f + (float) Math.sin(pulseAnimation * Math.PI * 2) * 0.15f;
            context.getMatrices().translate(x + 8, y + 8, 0);
            context.getMatrices().scale(scale, scale, 1f);
            context.getMatrices().translate(-8, -8, 0);
            x = 0;
            y = 0;
        }

        context.drawTexture(TEXTURE, x, y, 0, 0, 16, 16, 16, 16);

        // Color overlay based on temperature
        int color = getColorFromTemperature(temp);
        float alpha = isDangerous ? 0.7f + (float) Math.sin(pulseAnimation * Math.PI * 2) * 0.3f : 0.6f;

        RenderSystem.setShaderColor(
                ((color >> 16) & 0xFF) / 255f,
                ((color >> 8) & 0xFF) / 255f,
                (color & 0xFF) / 255f,
                alpha
        );
        context.drawTexture(TEXTURE, x, y, 0, 0, 16, 16, 16, 16);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        context.getMatrices().pop();

        // Recalculate x for text (if we applied transform)
        if (isDangerous) {
            x = screenWidth / 2 + 92;
            y = screenHeight - 50;
        }

        // Draw temperature text with better formatting
        String tempText = formatTemperature(temp);
        int textWidth = client.textRenderer.getWidth(tempText);
        int textX = x + (16 - textWidth) / 2;
        int textY = y + 20; // Below icon instead of on top

        // Add background for better readability
        context.fill(textX - 2, textY - 1, textX + textWidth + 2, textY + 9, 0x80000000);

        // Draw text with color based on severity
        int textColor = getTextColor(temp);
        context.drawTextWithShadow(client.textRenderer, tempText, textX, textY, textColor);
    }

    private String formatTemperature(int temp) {
        return temp + "°C";
    }

    private int getColorFromTemperature(int temp) {
        temp = MathHelper.clamp(temp, -50, 150);

        // Extreme cold (below -10)
        if (temp <= DANGER_COLD) {
            return 0x00FFFF; // Cyan
        }
        // Cold (below 15)
        else if (temp <= COMFORTABLE_LOW) {
            // Gradient from cyan to light blue
            float t = (temp - DANGER_COLD) / (float)(COMFORTABLE_LOW - DANGER_COLD);
            return lerpColor(0x00FFFF, 0x87CEEB, t);
        }
        // Comfortable (15-30)
        else if (temp <= COMFORTABLE_HIGH) {
            // Gradient from light blue to white
            float t = (temp - COMFORTABLE_LOW) / (float)(COMFORTABLE_HIGH - COMFORTABLE_LOW);
            return lerpColor(0x87CEEB, 0xFFFFFF, t);
        }
        // Warm (30-40)
        else if (temp <= HOT_THRESHOLD) {
            // Gradient from white to orange
            float t = (temp - COMFORTABLE_HIGH) / (float)(HOT_THRESHOLD - COMFORTABLE_HIGH);
            return lerpColor(0xFFFFFF, 0xFFA500, t);
        }
        // Hot (40-60)
        else if (temp <= DANGER_HOT) {
            // Gradient from orange to red
            float t = (temp - HOT_THRESHOLD) / (float)(DANGER_HOT - HOT_THRESHOLD);
            return lerpColor(0xFFA500, 0xFF0000, t);
        }
        // Extreme heat (above 60)
        else {
            return 0xFF0000; // Red
        }
    }

    private int getTextColor(int temp) {
        if (temp <= DANGER_COLD) return 0x00FFFF; // Cyan
        if (temp <= COLD_THRESHOLD) return 0x87CEEB; // Light blue
        if (temp <= COMFORTABLE_LOW) return 0xAAAAAA; // Gray
        if (temp <= COMFORTABLE_HIGH) return 0xFFFFFF; // White
        if (temp <= HOT_THRESHOLD) return 0xFFA500; // Orange
        if (temp <= DANGER_HOT) return 0xFF6347; // Tomato red
        return 0xFF0000; // Red
    }

    private int lerpColor(int color1, int color2, float t) {
        t = MathHelper.clamp(t, 0f, 1f);

        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int r = (int)(r1 + (r2 - r1) * t);
        int g = (int)(g1 + (g2 - g1) * t);
        int b = (int)(b1 + (b2 - b1) * t);

        return (r << 16) | (g << 8) | b;
    }
}