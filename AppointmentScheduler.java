import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


public class AppointmentScheduler {

    private static HashMap<Integer, List<Appointment>> docAppointments = new HashMap<>();
    private static HashMap<Integer, List<Appointment>> patientAppointments = new HashMap<>();
    private static String token = "1354b602-6e0c-432b-b939-68283e261980";

    /**
     * Main entry point of the AppointmentScheduler program.
     * Resets the scheduling system, retrieves the initial appointment state,
     * parses the existing appointments, and begins processing new appointment requests.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        resetSystem();
        String initialState = getInitialState();
        if(initialState == null) return;
        parseAppointments(initialState);
        scheduleAppointments();
    }

    /**
     * Sends a GET request to retrieve the initial scheduling state from the API.
     *
     * @return the response body containing the initial schedule in JSON format, or null if an error occurs
     */
    public static String getInitialState() {

        String url = "https://scheduling.interviews.brevium.com/api/Scheduling/Schedule?token=" + token;
        
        HttpClient client = HttpClient.newHttpClient();

        // Create the request.
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url)) 
                .GET() 
                .build();

        // Send the request and get the response
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();
            // Inform user of status of request. 
            if(status == 200) {
                System.out.println("Request was successful");
                return response.body();
            }
            else {
                System.out.println("There was an error with your request.");
                return null;
            }
        
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * Parses the initial JSON-formatted schedule and populates both doctor and patient appointment maps.
     *
     * @param initialState a JSON string representing the initial appointment data
     */
    public static void parseAppointments(String initialState) {
        // Remove brackets and split by "},{"
        String[] items = initialState.substring(2, initialState.length() - 1).split("\\},\\{");
        
        for(String appointment : items) {
            String[] arguments = appointment.split(",");

            int doctorId = Integer.parseInt(arguments[0].split(":")[1]);
            int personId = Integer.parseInt(arguments[1].split(":")[1]);

            ZoneId utc = ZoneId.of("UTC");
            ZonedDateTime appointmentTime = ZonedDateTime.parse(arguments[2].split("\"")[3]).withZoneSameInstant(utc);

            boolean isNewPatientAppointment = Boolean.parseBoolean(arguments[3].split(":")[1]);

            // Add to doc appointments.
            if(!docAppointments.containsKey(doctorId)) {
                docAppointments.put(doctorId, new ArrayList<Appointment>());
            }
            docAppointments.get(doctorId).add(new Appointment(doctorId, personId, appointmentTime, isNewPatientAppointment));

            // Add to patient appointments.
            if(!patientAppointments.containsKey(personId)) {
                patientAppointments.put(personId, new ArrayList<>());
            }
            patientAppointments.get(personId).add(new Appointment(doctorId, personId, appointmentTime, isNewPatientAppointment));
        }
    }

    /**
     * Sends a POST request to reset the scheduling system to its initial state.
     */
    public static void resetSystem() {
        String url = "https://scheduling.interviews.brevium.com/api/Scheduling/Start?token=" + token;

        HttpClient client = HttpClient.newHttpClient();

        // Build the POST request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody()) 
                .build();

        // Send the request.
        try {
            client.send(request, HttpResponse.BodyHandlers.ofString());
        
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Continuously fetches appointment requests, schedules them, and posts them back to the system.
     */
    public static void scheduleAppointments() {
        HttpResponse<String> response = getAppointmentRequest();  
        while(response != null && response.statusCode() == 200) {
            String[] appointRequest = response.body().substring(1, response.body().length() - 1).split(",");

            int requestId = Integer.parseInt(appointRequest[1].split(":")[1]);
            int personId = Integer.parseInt(appointRequest[1].split(":")[1]);
            List<String> preferredDays = null;
            List<Integer> preferredDocs = null;
            boolean isNew = false;

            // I ran out of time an was unable to finish adding functionally for converting the request into an appointment request object. :(

            Appointment apt = scheduleAppointment(new AppointmentRequest(requestId, personId, preferredDays, preferredDocs, isNew));
            markAsTaken(apt, requestId); // Make post request to inform scheduling system of added appoint. 

            response = getAppointmentRequest(); // Get the next appoint request.
        }
    }

    /**
     * Sends a POST request to mark an appointment as taken in the scheduling system.
     *
     * @param appt      the appointment to mark as scheduled
     * @param requestId the request ID corresponding to the appointment request
     */
    private static void markAsTaken(Appointment appt, int requestId) {
        try {
            // Build JSON body
            String json = "{"
                    + "\"doctorId\": " + appt.doctorID + ","
                    + "\"personId\": " + appt.personID + ","
                    + "\"appointmentTime\": \"" + appt.appointmentTime.toString() + "\","
                    + "\"isNewPatientAppointment\": " + appt.inNewPatientAppointment + ","
                    + "\"requestId\": " + requestId
                    + "}";

            String url = "https://scheduling.interviews.brevium.com/api/Scheduling/Schedule?token=" + token;

            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json") // important
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Response status: " + response.statusCode());
            System.out.println("Response body: " + response.body());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Fetches the next appointment request from the scheduling system API.
     *
     * @return the HTTP response containing the appointment request data, or null if the request fails
     */
    public static HttpResponse<String> getAppointmentRequest() {
        String url = "https://scheduling.interviews.brevium.com/api/Scheduling/AppointmentRequest?token=" + token;
        
        HttpClient client = HttpClient.newHttpClient();

        // Create the request.
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url)) 
                .GET() 
                .build();

        // Send the request and get the response
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * Attempts to schedule a valid appointment for a given appointment request.
     * Prioritizes preferred days, then searches all valid weekdays in Nov/Dec 2021.
     *
     * @param request the appointment request to schedule
     * @return a valid Appointment object, or null if no valid time is found
     */
    public static Appointment scheduleAppointment(AppointmentRequest request) {

        ZoneId utc = ZoneId.of("UTC");

        // Try preferred days first
        for (String preferredDayStr : request.preferredDays) {
            ZonedDateTime preferred = ZonedDateTime.parse(preferredDayStr).withZoneSameInstant(utc);

            Appointment appt = tryScheduleOnDay(preferred, request, docAppointments, patientAppointments);
            if (appt != null) return appt;
        }

        // Fallback: iterate all weekdays in Nov/Dec 2021
        for (Month month : new Month[]{Month.NOVEMBER, Month.DECEMBER}) {
            for (int day = 1; day <= month.length(Year.isLeap(2021)); day++) {
                LocalDate date = LocalDate.of(2021, month, day);
                DayOfWeek dow = date.getDayOfWeek();
                if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) continue;

                for (int hour : getAllowedHours(request.isNew)) {
                    ZonedDateTime candidateTime = date.atTime(hour, 0).atZone(utc);
                    Appointment appt = tryScheduleAtTime(candidateTime, request, docAppointments, patientAppointments);
                    if (appt != null) return appt;
                }
            }
        }

        // No available slot. This will never happen. The specs provided said there would always be a valid slot. 
        return null;
    }

    /**
     * Attempts to schedule an appointment on a specific day, checking allowed hours.
     *
     * @param day the day to check
     * @param request the appointment request to fulfill
     * @param doctorAppointments current doctor appointment map
     * @param patientAppointments current patient appointment map
     * @return a valid Appointment object if available, otherwise null
     */
    private static Appointment tryScheduleOnDay(ZonedDateTime day,
        AppointmentRequest request,
        HashMap<Integer, List<Appointment>> doctorAppointments,
        HashMap<Integer, List<Appointment>> patientAppointments) {

        for (int hour : getAllowedHours(request.isNew)) {
            ZonedDateTime candidateTime = day.withHour(hour).withMinute(0).withSecond(0).withNano(0);
            Appointment appt = tryScheduleAtTime(candidateTime, request, doctorAppointments, patientAppointments);
            if (appt != null) return appt;
        }
        return null;
    }

    /**
     * Attempts to schedule an appointment at a specific time for the given request.
     * Checks both doctor availability and patient spacing constraints.
     *
     * @param candidateTime the time to attempt scheduling the appointment
     * @param request the appointment request details
     * @param doctorAppointments map of doctor appointments
     * @param patientAppointments map of patient appointments
     * @return a scheduled Appointment object if successful, otherwise null
     */
    private static Appointment tryScheduleAtTime(ZonedDateTime candidateTime,
        AppointmentRequest request,
        HashMap<Integer, List<Appointment>> doctorAppointments,
        HashMap<Integer, List<Appointment>> patientAppointments) {

        int doctorId = request.preferredDocs.get(0);

        // Doctor availability
        List<Appointment> docAppts = doctorAppointments.getOrDefault(doctorId, new ArrayList<>());
        for(Appointment a : docAppts) {
            if(a.appointmentTime.equals(candidateTime)) {
                return null; // The doctor isn't free during the specified time. 
            }
        }

        // Patient spacing
        List<Appointment> patAppts = patientAppointments.getOrDefault(request.personId, new ArrayList<>());

        // Check each of the patient’s existing appointments
        for (Appointment existingAppt : patAppts) {
            ZonedDateTime existingTime = existingAppt.appointmentTime;
            long daysBetween = Math.abs(Duration.between(existingTime, candidateTime).toDays());

            // If the candidate time is less than 7 days from an existing appointment, patient is not free
            if (daysBetween < 7) {
                return null; // Cannot schedule at this time. Not more than seven days from another appoint. 
            }
        }

        // Schedule appointment
        Appointment newAppt = new Appointment(doctorId, request.personId, candidateTime, request.isNew);
        doctorAppointments.computeIfAbsent(doctorId, k -> new ArrayList<>()).add(newAppt);
        patientAppointments.computeIfAbsent(request.personId, k -> new ArrayList<>()).add(newAppt);

        return newAppt;
    }

    /**
     * Returns a list of valid scheduling hours depending on whether the appointment is for a new patient.
     *
     * @param isNew true if the appointment is for a new patient
     * @return a list of valid hour integers (in UTC)
     */
    private static List<Integer> getAllowedHours(boolean isNew) {
        if (isNew) return Arrays.asList(15, 16); // 3pm & 4pm
        List<Integer> hours = new ArrayList<>();
        for (int h = 8; h <= 16; h++) hours.add(h);
        return hours;
    }
    
}

