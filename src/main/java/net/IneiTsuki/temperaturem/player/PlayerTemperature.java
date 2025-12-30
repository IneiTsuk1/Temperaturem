package net.IneiTsuki.temperaturem.player;

public class PlayerTemperature {

    private double temperature = 0.0;

    public int get() {
        return (int) Math.round(temperature);
    }

    public double getExact() {
        return temperature;
    }

    public void set(int value) {
        this.temperature = value;
    }

    public void setExact(double value) {
        this.temperature = value;
    }

    public void add(double delta) {
        this.temperature += delta;
    }

    /**
     * Gradually moves temperature toward zero at the specified rate.
     * @param rate The rate at which to approach zero (degrees per call)
     */
    public void approachZero(double rate) {
        if (temperature > rate) {
            temperature -= rate;
        } else if (temperature < -rate) {
            temperature += rate;
        } else {
            temperature = 0.0; // Snap to zero when very close
        }
    }

    /**
     * Legacy method for backward compatibility.
     * Approaches zero at a rate of 1.0 per call.
     */
    @Deprecated
    public void approachZero() {
        approachZero(1.0);
    }
}