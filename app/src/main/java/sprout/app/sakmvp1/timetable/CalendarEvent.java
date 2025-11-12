package sprout.app.sakmvp1.timetable;

import java.io.Serializable;
import java.time.LocalDate;

public class CalendarEvent implements Serializable {
    public String documentId;
    public String title;
    public String startDate;    // "2025-11-10"
    public String endDate;      // "2025-11-12"
    public String startTime;    // [변경] 시작 시간 "20:00"
    public String endTime;      // [변경] 종료 시간 "21:00"
    public String description;
    public int color;
    public boolean isYearly;    // [추가] 매년 반복 여부

    public CalendarEvent() {}

    public CalendarEvent(String title, String startDate, String endDate, String startTime, String endTime, String description, int color, boolean isYearly) {
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.description = description;
        this.color = color;
        this.isYearly = isYearly;
    }

    public LocalDate getStartLocalDate() { return LocalDate.parse(startDate); }
    public LocalDate getEndLocalDate() { return LocalDate.parse(endDate); }
}