// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;

public final class FindMeetingQuery {
  /**
   * Given a Collection of all known events and a meeting request, returns the TimeRanges where
   * the meeting can occur. In the TimeRanges returned, all mandatory meeting attendees will be 
   * able to attend. If there is at least one timeslot where ALL mandatory and ALL optional attendees 
   * can attend, then those timeslot(s) will be returned.
   * 
   * Algorithm:
   * 1. Going through all the events, add events that involve meeting attendees (both optional and 
   * mandatory) to an ArrayList, eventList.
   * 2. Sort eventList according to event start times
   * 3. Merge adjacent events if they overlap
   * 4. Going through the merged events list, add the gaps between events that are longer or 
   * equal to meeting duration to output
   * 5. If there are no meeting times, remove optional attendees, and repeat steps 1-4.
   * 
   * @param events Collection of all known events in the day
   * @param request the MeetingRequest containing the details of the meeting 
   * @return the Collection of TimeRanges when the meeting can be scheduled
   */
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {

    ArrayList<TimeRange> meetingTimes = new ArrayList<TimeRange>();

    // EDGE CASE: No meetingTimes available for meetings longer than 1 day
    if (request.getDuration() > TimeRange.WHOLE_DAY.duration()) {
      return meetingTimes;
    }
    
    /* FILTER ALL EVENTS BY ATTENDEES */
    // Initially consider both mandatory and optional attendees
    HashSet<String> meetingAttendees = new HashSet<String>(request.getAttendees());
    meetingAttendees.addAll(request.getOptionalAttendees());
    ArrayList<Event> eventList = filterEventsByAttendees(events, meetingAttendees);

    /* SORT EVENTS BY START TIME */ 
    Comparator<Event> compareByStartTime = 
        (Event event1, Event event2) -> Integer.compare(event1.getWhen().start(), event2.getWhen().start());
    eventList.sort(compareByStartTime);

    /* MERGE ANY OVERLAPPING EVENTS */ 
    ArrayList<TimeRange> mergedEventTimes = mergeOverlappingEvents(eventList);
    
    /* RETURN ALL THE BLOCKS OF AVAILABILITY */
    meetingTimes = findAvailability(mergedEventTimes, request.getDuration());

    // If at this point there are no possible meeting times, try removing optional guests
    // and repeat the process.
    if (meetingTimes.isEmpty()) {
      meetingAttendees.removeAll(request.getOptionalAttendees());
      eventList = filterEventsByAttendees(events, meetingAttendees);
      eventList.sort(compareByStartTime);
      mergedEventTimes = mergeOverlappingEvents(eventList);
      meetingTimes = findAvailability(mergedEventTimes, request.getDuration());
    }
    return meetingTimes;
  }

  /**
   * Returns the subset of events in which the attendees provided are attending.
   * @param events the collection of Event objects to be filtered
   * @param attendees the attendees to filter the Event objects by
   * @return the subset of events in which the attendees are attending
   */
  private ArrayList<Event> filterEventsByAttendees(Collection<Event> events, Collection<String> attendees) {
    ArrayList<Event> eventList = new ArrayList<>();
    for (Event event : events) {
      HashSet<String> eventAttendees = new HashSet<String>(event.getAttendees());
      HashSet<String> combinedAttendees = new HashSet<String>(attendees);
      combinedAttendees.retainAll(eventAttendees);
      if (!combinedAttendees.isEmpty()) {
        eventList.add(event);
      }
    }
    return eventList;
  }

  /**
   * Merges overlapping events and returns the ArrayList of TimeRanges representing the
   * time blocks where events take place in chronological order.
   * @param eventList the ArrayList of events sorted in chronological order by start time
   * @return the ArrayList of TimeRanges representing the time blocks where events occur
   */
  private ArrayList<TimeRange> mergeOverlappingEvents(ArrayList<Event> eventList) {
    ArrayList<TimeRange> mergedEventTimes = new ArrayList<>();
    
    if (eventList.isEmpty()) {
      return mergedEventTimes;
    }

    mergedEventTimes.add(eventList.get(0).getWhen());

    for (int i = 1; i < eventList.size(); i++) {
      // For two adjacent events, check if they overlap
      TimeRange lastTimeRange = mergedEventTimes.get(mergedEventTimes.size() - 1);
      TimeRange currEventTime = eventList.get(i).getWhen(); 
      if (lastTimeRange.overlaps(currEventTime)) {
        // If they do, create new TimeRange with combined time and add to mergedEventTimes,
        // replacing the original TimeRange the new event merged with
        int newEnd = Math.max(lastTimeRange.end(), currEventTime.end());
        TimeRange combinedTime = TimeRange.fromStartEnd(lastTimeRange.start(), newEnd, false);
        mergedEventTimes.remove(mergedEventTimes.size() - 1);
        mergedEventTimes.add(combinedTime);
      }
      else {
        // Otherwise, add the current event to the list
        mergedEventTimes.add(currEventTime);
      }
    }
    return mergedEventTimes;
  }

  /**
   * Given a sorted list of TimeRanges where events take place, return a list of the availability
   * (no event) times from the start of the day (00:00) to the end of the day (23:59) that are 
   * at least of the given duration.
   * @param eventTimes the sorted list of TimeRanges that represent the time periods in the day
   *     when events take place
   * @param duration the minimum length of the period of availability
   * @return TimeRanges instances representing the time periods when no events take place, 
   *     that are at least of the given duration
   */
  private ArrayList<TimeRange> findAvailability(ArrayList<TimeRange> eventTimes, long duration) {
    ArrayList<TimeRange> availability = new ArrayList<TimeRange>();
    
    // Start at the beginning of the day
    int timeslotStart = TimeRange.START_OF_DAY;

    for (int i = 0; i < eventTimes.size(); i++) {
      // If the gap between timeSlotstart and the start of the next event is >= meeting duration,
      // add to availability
      TimeRange currEvent = eventTimes.get(i);
      if (currEvent.start() - timeslotStart >= duration) {
        TimeRange timeslot = TimeRange.fromStartEnd(timeslotStart, currEvent.start(), false);
        availability.add(timeslot);
      }
      // the next potential meeting time will start from the end of the current event
      timeslotStart = currEvent.end();
    }

    // Finally, add the timeslot from the end of the last event to the end of the day
    // timeslotStart will be set to the end of the last event OR the start of the day if no events
    if (TimeRange.END_OF_DAY - timeslotStart >= duration) {
      TimeRange timeslot = TimeRange.fromStartEnd(timeslotStart, TimeRange.END_OF_DAY, true);
      availability.add(timeslot);
    }
    return availability;
  }
}
