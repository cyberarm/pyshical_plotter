package org.greece.statues;

import com.qualcomm.robotcore.hardware.DcMotor;

public class Motor {
    private long lastUpdateMs;
    private int lastPosition;
    private double lastVelocity, currentVelocity;
    private DcMotor motor;
    private boolean hasUpdatedBefore = false;

    public Motor(DcMotor motor) {
        this.motor = motor;
        this.lastUpdateMs = 0;
        this.lastPosition = 0;
        this.lastVelocity = 0;
    }

    public void update() {
        if (hasUpdatedBefore) {
            currentVelocity = (motor.getCurrentPosition() - lastPosition) / ((System.currentTimeMillis() - lastUpdateMs)/1000.0);
        } else { hasUpdatedBefore = true; }

        lastVelocity = currentVelocity;
        lastPosition = motor.getCurrentPosition();
        lastUpdateMs = System.currentTimeMillis();
    }

    public double velocity() {
        return currentVelocity;
    }

    public String name() {
       return motor.getDeviceName();
    }

    public double power() {
       return motor.getPower();
    }

    public int position() {
        return motor.getCurrentPosition();
    }

    public int lastPosition() {
        return lastPosition;
    }

    public DcMotor getMotor() {
        return motor;
    }

    public double lastVelocity() {
        return lastVelocity;
    }
}