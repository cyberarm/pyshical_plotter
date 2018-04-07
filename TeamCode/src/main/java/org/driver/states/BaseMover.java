package org.driver.states;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.TouchSensor;

import org.engine.State;
import org.engine.Support;

import java.util.HashMap;

public class BaseMover extends State {
    DcMotor xAxis, yAxis;
    TouchSensor xAxisEndStop, yAxisEndStop;

    int targetX, targetY, localX, localY;
    int multiplier = 40, fuzz;
    double speed = 0.1;
    boolean setupCompleted = false, checkEndStops = false;
    HashMap encoderData = new HashMap<>();

    // What to do when a encoder malfunction or motor stall is detected
    int ABORT = 0,
        SKIP  = 1,
        IGNORE= 2;
    int HALT_MOTOR = 0;

    public BaseMover(int x, int y) {
        targetX = x;
        targetY = y;

        xAxis = engine.hardwareMap.dcMotor.get("xAxis");
        yAxis = engine.hardwareMap.dcMotor.get("yAxis");

        xAxisEndStop = engine.hardwareMap.touchSensor.get("xAxisEndStop");
        yAxisEndStop = engine.hardwareMap.touchSensor.get("yAxisEndStop");

        xAxis.setPower(0);
        yAxis.setPower(0);

        localX = adjustedTarget(targetX);
        localY = adjustedTarget(targetY);
        fuzz = 10;
    }

    @Override
    public void exec() {
        if (!setupCompleted) {
            Support.puts("BaseMover", "Moving pen to " + localX + ":" + localY + "...");
            setupCompleted = true;
        } // else { Support.puts("BaseMover", ""+xAxis.getCurrentPosition()+":"+yAxis.getCurrentPosition()); }

        if (checkEndStops) { // Used for Home state
            checkEncoder("xAxis", xAxis, targetX, 150, ABORT);
            checkEncoder("yAxis", yAxis, targetY, 150, ABORT);

            if (checkEndStop("xAxisEndStop", xAxisEndStop)) {
                xAxis.setPower(0.0);
            } else {
                xAxis.setPower(-0.1);
            }

            if (checkEndStop("yAxisEndStop", yAxisEndStop)) {
                yAxis.setPower(0.0);
            } else {
                yAxis.setPower(-0.1);
            }

            if (xAxis.getPower() == 0.0 && yAxis.getPower() == 0.0) {
                setFinished(true);
                return;
            }

        } else {
            checkEncoder("xAxis", xAxis, targetX, 150, ABORT);
            checkEncoder("yAxis", yAxis, targetY, 150, ABORT);

            if (!between(yAxis.getCurrentPosition(), adjustedTarget(targetY), fuzz)) {
                if (yAxis.getCurrentPosition() < localY + fuzz) {
                    yAxis.setPower(-speed);
                } else if (yAxis.getCurrentPosition() > localY - fuzz) {
                    yAxis.setPower(speed);

                } else {
                    yAxis.setPower(0);
                }
            } else {
                if (xAxis.getCurrentPosition() < localX + fuzz) {
                    xAxis.setPower(-speed);
                } else if (xAxis.getCurrentPosition() > localX + fuzz) {
                    xAxis.setPower(speed);

                } else {
                    xAxis.setPower(0);
                    setFinished(true);
                }
            }
        }
    }

    @Override
    public void stop() {
        xAxis.setPower(0);
        yAxis.setPower(0);
    }

    protected void useEndStops() {
//        checkEndStops = true;
    }

    private boolean checkEndStop(String friendlyName, TouchSensor touchSensor) {
        if (touchSensor.isPressed()) {
            Support.puts("BaseMover", ""+friendlyName+" is pressed.");
            return true;
        } else {
            return false;
        }
    }

    private void checkEncoder(String friendlyName, DcMotor motor, int target, int threshold, int resolver) {
        if (between(motor.getCurrentPosition(), target, fuzz)) {
            // All clear
            if (encoderData.get(friendlyName) != null) {
                encoderData.put(friendlyName, ""+motor.getCurrentPosition()+":"+0);
            }
        } else {
            if (encoderData.get(friendlyName) != null) {
                // last_position:same_position_count
                String[] data = ((String) encoderData.get(friendlyName)).split(":");
                int last_position = Integer.valueOf(data[0]);
                int same_position_count = Integer.valueOf(data[1]);

                if (between(motor.getCurrentPosition(), last_position, fuzz)) {
                    if (threshold >= same_position_count) {
                        resolve(friendlyName, resolver); // Explode.
                    }

                    same_position_count++;
                    encoderData.put(friendlyName, ""+last_position+":"+same_position_count);
                }
            } else {
                encoderData.put(friendlyName, ""+motor.getCurrentPosition()+":"+0);
            }
        }
    }

    private void resolve(String name, int resolver) {
        if (resolver == ABORT) {
//            throw new RuntimeException("Encoder on "+name+" is broken or the motor is stalled!");
            engine.telemetry.addData("Error", "Encoder on "+name+" is broken or the motor is stalled!"+engine.getRuntime());
            setFinished(true);
            engine.stop();

        } else if (resolver == SKIP) {
            Support.puts("BaseMover", "Encoder or motor error on "+name+"! Skipping.");
            setFinished(true);
        } else if (resolver == IGNORE) {
            // Do nothing but watch the world burn.
        } else {
            Support.puts("BaseMover", "Unknown resolver: "+resolver);
        }
    }

    /*
        position.between?(target-fuzz, target+fuzz)
     */

    private boolean between(int position, int target, int fuzz) {
        if ((position > target-fuzz) && (position < target+fuzz)) {
            return true;
        } else {
            return false;
        }
    }

    private int adjustedTarget(int target) {
        return (target*multiplier);
    }
}
