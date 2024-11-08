package frc.team3128.subsystems;

import common.core.subsystems.ManipulatorTemplate;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.team3128.subsystems.Intake.IntakeState;
import frc.team3128.subsystems.Shooter.ShooterState;

import static frc.team3128.Constants.HopperConstants.*;
import static frc.team3128.Constants.Flags.*;

public class Hopper extends ManipulatorTemplate {

    public enum HopperState {
        INTAKE(HPPR_INTAKE_POWER),
        OUTTAKE(HPPR_OUTTAKE_POWER),
        IDLE(HPPR_STALL_POWER),
        ADVANCE(HPPR_STALL_POWER);

        private final double power;

        private HopperState(double power) {
            this.power = power;
        }

        public double getPower() {
            return power;
        }
    }

    private static Hopper instance;
    private static HopperState goalState;

    public static synchronized Hopper getInstance() {
        if (instance == null) 
            instance = new Hopper();
        return instance;
    }

    private Hopper() {
        super(STALL_CURRENT, HPPR_INTAKE_POWER, HPPR_OUTTAKE_POWER, HPPR_STALL_POWER, 0.3, HPPR_MOTOR);

        // initShuffleboard();

        setDefaultCommand(setState(HopperState.IDLE));
    }

    @Override
    protected void configMotors() {
        HPPR_MOTOR.setUnitConversionFactor(HPPR_GEAR_RATIO);
        HPPR_MOTOR.setInverted(HPPR_MOTOR_INVERT);
        HPPR_MOTOR.setCurrentLimit(HPPR_CURRENT_LIMIT);
        HPPR_MOTOR.setNeutralMode(HPPR_NEUTRAL_MODE);
        HPPR_MOTOR.setStatusFrames(HPPR_STATUS_FRAME);
        HPPR_MOTOR.enableVoltageCompensation(HPPR_VOLT_COMP);
    }

    public void configTriggers() {

        new Trigger(not(hopperHasNote))
        .and(()-> Intake.goalStateIs(IntakeState.INTAKE))
        .onTrue(setState(HopperState.INTAKE));

        new Trigger(noteAdvanceRequired)
        .onTrue(setState(HopperState.ADVANCE));

        // necessary to allow for two note intaking
        new Trigger(hasTwoNotes)
        .or(()->Intake.goalStateIs(IntakeState.RETRACTED) &&
                Shooter.goalStateIs(ShooterState.IDLE))
        .onTrue(setState(HopperState.IDLE));
    }

    @Override
    public boolean hasObjectPresent() {
        return !HPPR_SENSOR.get();
    }

    public Command setState(HopperState state) {
        goalState = state;
        return runManipulator(state.getPower());
    }

    public static HopperState getGoalState() {
        return goalState;
    }

    public static boolean goalStateIs(HopperState state) {
        return goalState == state;
    }
}
