package net.IneiTsuki.temperaturem.player;

public class PlayerTemperature {

    private double temperature = 0.0; // Changed from int to double

    public int get() {
        return (int) Math.round(temperature); // Return rounded value
    }

    public double getExact() {
        return temperature; // Get precise value
    }

    public void set(int value) {
        this.temperature = value;
    }

    public void setExact(double value) {
        this.temperature = value;
    }

    public void add(int delta) {
        this.temperature += delta;
    }

    public void approachZero() {
        if (temperature > 0) temperature--;
        else if (temperature < 0) temperature++;
    }
}