package org.example;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

class TimeSlot {
    private String start;
    private String end;

    // No-argument constructor needed for Jackson
    public TimeSlot() {}

    // Constructor with fields
    @JsonCreator // If you're using this, you don't need the no-argument constructor
    public TimeSlot(@JsonProperty("start") String start, @JsonProperty("end") String end) {
        this.start = start;
        this.end = end;
    }

    // Getters and setters
    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }
}


class PersonSchedule {
    public TimeSlot working_hours;
    public List<TimeSlot> planned_meeting;

    // Constructors, getters, setters
}

public class MeetingScheduler {

    public static List<TimeSlot> findAvailableSlots(PersonSchedule schedule) {
        List<TimeSlot> availableSlots = new ArrayList<>();
        TimeSlot workingHours = schedule.working_hours;
        LocalTime workStart = LocalTime.parse(workingHours.getStart());
        LocalTime workEnd = LocalTime.parse(workingHours.getEnd());

        // Assuming the planned_meeting list is sorted by start time
        LocalTime lastEnd = workStart;
        for (TimeSlot meeting : schedule.planned_meeting) {
            LocalTime meetingStart = LocalTime.parse(meeting.getStart());
            if (lastEnd.isBefore(meetingStart)) {
                availableSlots.add(new TimeSlot(lastEnd.toString(), meetingStart.toString()));
            }
            lastEnd = LocalTime.parse(meeting.getEnd());
        }

        // Check the end of the day
        if (lastEnd.isBefore(workEnd)) {
            availableSlots.add(new TimeSlot(lastEnd.toString(), workEnd.toString()));
        }

        return availableSlots;
    }

    public static List<TimeSlot> findOverlappingSlots(List<TimeSlot> slots1, List<TimeSlot> slots2) {
        List<TimeSlot> overlappingSlots = new ArrayList<>();

        for (TimeSlot slot1 : slots1) {
            for (TimeSlot slot2 : slots2) {
                LocalTime startMax = LocalTime.parse(slot1.getStart()).isAfter(LocalTime.parse(slot2.getStart()))
                        ? LocalTime.parse(slot1.getStart())
                        : LocalTime.parse(slot2.getStart());
                LocalTime endMin = LocalTime.parse(slot1.getEnd()).isBefore(LocalTime.parse(slot2.getEnd()))
                        ? LocalTime.parse(slot1.getEnd())
                        : LocalTime.parse(slot2.getEnd());

                if (startMax.isBefore(endMin)) {
                    overlappingSlots.add(new TimeSlot(startMax.toString(), endMin.toString()));
                }
            }
        }

        return overlappingSlots;
    }

    public static List<TimeSlot> calculatePossibleMeetingTimes(List<TimeSlot> overlappingSlots, String duration) {
        List<TimeSlot> meetingTimes = new ArrayList<>();
        Duration meetingDuration = Duration.between(LocalTime.MIN, LocalTime.parse(duration));

        for (TimeSlot slot : overlappingSlots) {
            LocalTime start = LocalTime.parse(slot.getStart());
            LocalTime end = LocalTime.parse(slot.getEnd());
            while (start.plus(meetingDuration).isBefore(end) || start.plus(meetingDuration).equals(end)) {
                meetingTimes.add(new TimeSlot(start.toString(), start.plus(meetingDuration).toString()));
                start = start.plusMinutes(1); // Assuming meetings can start on any minute increment
            }
        }

        return meetingTimes;
    }

    public static void main(String[] args) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        InputStream inputStream = MeetingScheduler.class.getClassLoader().getResourceAsStream("path_to_calendar1.json");
        InputStream inputStream2 = MeetingScheduler.class.getClassLoader().getResourceAsStream("path_to_calendar2.json");
        // Read JSON files into PersonSchedule objects
        PersonSchedule schedule1 = mapper.readValue(inputStream, PersonSchedule.class);
        PersonSchedule schedule2 = mapper.readValue(inputStream2, PersonSchedule.class);

        // Find available slots for each person
        List<TimeSlot> availableSlots1 = findAvailableSlots(schedule1);
        List<TimeSlot> availableSlots2 = findAvailableSlots(schedule2);

        // Find overlapping slots
        List<TimeSlot> overlappingSlots = findOverlappingSlots(availableSlots1, availableSlots2);

        // Calculate possible meeting times
        String meetingDuration = "00:30"; // This should be read as an input or from a file
        List<TimeSlot> possibleMeetingTimes = calculatePossibleMeetingTimes(overlappingSlots, meetingDuration);

        // Print out the possible meeting times
        for (TimeSlot slot : possibleMeetingTimes) {
            System.out.println("[" + slot.getStart() + ", " + slot.getEnd() + "]");
        }
    }
}
