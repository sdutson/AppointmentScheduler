import java.util.List;

public class AppointmentRequest {
    public int requestId;
    public int personId;
    public List<String> preferredDays;
    public List<Integer> preferredDocs;
    public boolean isNew;

    public AppointmentRequest(int requestId, int personID, List<String> preferredDays,
    List<Integer> preferredDocs, boolean isNew) {
        this.requestId = requestId;
        this.personId = personID;
        this.preferredDays = preferredDays;
        this.preferredDocs = preferredDocs;
        this.isNew = isNew;
    }
}
