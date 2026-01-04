package net.IneiTsuki.temperaturem.client;

public class ClientPlayerTemperature {
    private static int targetTemperature = 0;
    private static float displayTemperature = 0f;
    private static final float LERP_SPEED = 0.1f; // Smooth transition speed

    public static int get() {
        return Math.round(displayTemperature);
    }

    public static float getExact() {
        return displayTemperature;
    }

    public static void set(int temp) {
        targetTemperature = temp;
    }

    public static void tick() {
        if (displayTemperature != targetTemperature) {
            displayTemperature += (targetTemperature - displayTemperature) * LERP_SPEED;

            // Snap to target if very close
            if (Math.abs(targetTemperature - displayTemperature) < 0.1f) {
                displayTemperature = targetTemperature;
            }
        }
    }
}