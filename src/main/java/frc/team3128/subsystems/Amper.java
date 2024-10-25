package frc.team3128.subsystems;

import static frc.team3128.Constants.AmperConstants.*;

import common.core.controllers.Controller;
import common.core.controllers.TrapController;
import common.core.subsystems.ElevatorTemplate;
import common.core.subsystems.ShooterTemplate;
import common.hardware.motorcontroller.NAR_CANSpark;
import common.hardware.motorcontroller.NAR_CANSpark.ControllerType;
import common.hardware.motorcontroller.NAR_CANSpark.SparkMaxConfig;
import common.hardware.motorcontroller.NAR_Motor.Neutral;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import common.utility.shuffleboard.NAR_Shuffleboard;

import static edu.wpi.first.wpilibj2.command.Commands.sequence;
import static edu.wpi.first.wpilibj2.command.Commands.waitSeconds;
import static edu.wpi.first.wpilibj2.command.Commands.waitUntil;

public class Amper extends SubsystemBase{
    private final NAR_CANSpark elevatorMotor = new NAR_CANSpark(ELEV_MOTOR_ID);
    private final NAR_CANSpark rollerMotor = new NAR_CANSpark(ROLLER_MOTOR_ID, ControllerType.CAN_SPARK_FLEX);
    public class AmpElevator extends ElevatorTemplate {
    
        private AmpElevator() {
            super(new TrapController(ELEVATOR_PID, TRAP_CONSTRAINTS), elevatorMotor);
            setTolerance(POSITION_TOLERANCE);
            setConstraints(MIN_SETPOINT, MAX_SETPOINT);
            // initShuffleboard();
        }
    
        @Override
        protected void configMotors(){
            elevatorMotor.setUnitConversionFactor(UNIT_CONV_FACTOR);
            elevatorMotor.setCurrentLimit(CURRENT_LIMIT);
            elevatorMotor.setInverted(true);
            elevatorMotor.setNeutralMode(Neutral.BRAKE);
            elevatorMotor.setStatusFrames(SparkMaxConfig.POSITION);
        }
    
    }
    
    public class AmpManipulator extends ShooterTemplate {
    
        private AmpManipulator() {
            super(new Controller(ROLLER_PID, Controller.Type.VELOCITY), rollerMotor);
            setTolerance(ROLLER_TOLERANCE);
            setConstraints(ROLLER_MIN_RPM, ROLLER_MAX_RPM);
            // initShuffleboard();
        }
    
        @Override
        protected void configMotors(){
            rollerMotor.setVelocityStatusFrames();
            rollerMotor.setInverted(true);
            rollerMotor.setNeutralMode(Neutral.COAST);
            rollerMotor.setCurrentLimit(CURRENT_LIMIT);
        }
    }
    

    public enum AmpState {
        EXTENDED(21.25, 5500),
        PRIMED(21.25*0.7, 5500),
        IDLE(0, 0, true);

        private double elevatorSetpoint;
        private double rollerSetpoint;
        private boolean disableOnCompletion;

        private AmpState(double elevatorSetpoint, double rollerSetpoint, boolean disableOnCompletion){
            this.elevatorSetpoint = elevatorSetpoint;
            this.rollerSetpoint = rollerSetpoint;
            this.disableOnCompletion = disableOnCompletion;
        }
       
        private AmpState(double elevatorSetpoint, double rollerSetpoint) {
            this.elevatorSetpoint = elevatorSetpoint;
            this.rollerSetpoint = rollerSetpoint;
        }

        public double getElevatorSetpoint() {
            return elevatorSetpoint;
        }

        public double getRollerSetpoint() {
            return rollerSetpoint;
        }

        public boolean disableOnCompletion() {
            return disableOnCompletion;
        }
    }

    private static Amper instance;

    public AmpElevator elevator;
    public AmpManipulator manipulator;


    public static synchronized Amper getInstance() {
        if (instance == null)
            instance = new Amper();
        return instance;
    }

    private Amper() {
        elevator = new AmpElevator();
        manipulator = new AmpManipulator();

        NAR_Shuffleboard.addData(getName(), "At Sepoint", ()-> atSetpoint(), 2, 2);
        NAR_Shuffleboard.addData(getName(), "IDLE", ()-> isState(Amper.AmpState.IDLE), 0, 0);
        NAR_Shuffleboard.addData(getName(), "Primed", ()-> isState(Amper.AmpState.PRIMED), 1, 0);
        NAR_Shuffleboard.addData(getName(), "Extended", ()-> isState(Amper.AmpState.EXTENDED), 0, 1);
    }

    public Command setState(AmpState state) {
        return setState(state, 0);
    }

    public Command setState(AmpState state, double delay) {
        return sequence(
            manipulator.shoot(state.getRollerSetpoint()),
            elevator.moveElevator(state.getElevatorSetpoint()),
            waitSeconds(delay),
            waitUntil(()-> atSetpoint()),
            Commands.either(disable(), waitUntil(()-> atSetpoint()), ()-> state.disableOnCompletion())
        );
    }

    public boolean isState(AmpState state) {
        return state.getElevatorSetpoint() == elevator.getSetpoint()
        && state.getRollerSetpoint() == manipulator.getSetpoint()
        && atSetpoint();
    }

    public boolean atSetpoint() {
        return elevator.atSetpoint() && manipulator.atSetpoint();
    }

    public  Command reset() {
        return sequence(
            disable(),
            elevator.reset(0)
        );
    }

    public Command disable() {
        return sequence(
            Commands.runOnce(()-> elevator.disable()),
            Commands.runOnce(()-> manipulator.disable())
        ).ignoringDisable(true);
    }
}