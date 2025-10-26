import java.time.ZonedDateTime;

public class Appointment {
    protected int doctorID;
    protected int personID;
    protected ZonedDateTime appointmentTime;
    protected boolean inNewPatientAppointment;

        public Appointment(int doctorID, int personID, ZonedDateTime appointmentTime
        , boolean inNewPatientAppointment) {
            this.doctorID = doctorID;
            this.personID = personID;
            this.appointmentTime = appointmentTime;
            this.inNewPatientAppointment = inNewPatientAppointment;
        }
    }