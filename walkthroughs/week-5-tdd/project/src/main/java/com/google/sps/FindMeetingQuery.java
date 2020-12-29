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
   * the meeting can occur.
   * 
   * Algorithm:
   * 1. Going through all the events, add events that involve meeting attendees to an ArrayList, eventList
   * 2. Sort eventList according to event start times
   * 3. Merge adjacent events if they overlap
   * 4. Going through the merged events list, add the gaps between events that are longer or 
   * equal to meeting duration to output
   */
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {

    ArrayList<Event> eventList = new ArrayList<>();
    ArrayList<TimeRange> meetingTimes = new ArrayList<TimeRange>();

    // EDGE CASE: No meetingTimes available for meetings longer than 1 day
    if (request.getDuration() > TimeRange.WHOLE_DAY.duration()) {
      return meetingTimes;
    }

    // Only care about events of people that are attendees of the meeting
    HashSet<String> meetingAttendees = new HashSet<String>(request.getAttendees());
    for (Event event : events) {
      HashSet<String> eventAttendees = new HashSet<String>(event.getAttendees());
      HashSet<String> combinedAttendees = new HashSet<String>(meetingAttendees);
      combinedAttendees.retainAll(eventAttendees);
      if (!combinedAttendees.isEmpty()) {
        eventList.add(event);
      }
    }

    // EDGE CASE: The whole day is free if there were no events
    if (eventList.size() == 0) {
      meetingTimes.add(TimeRange.WHOLE_DAY);
      return meetingTimes;
    }

    /* SORT EVENTS BY START TIME */ 
    Comparator<Event> compareByStartTime = 
        (Event event1, Event event2) -> Integer.compare(event1.getWhen().start(), event2.getWhen().start());
    
    eventList.sort(compareByStartTime);

    /* MERGE ANY OVERLAPPING EVENTS */ 
    ArrayList<TimeRange> mergedEventTimes = new ArrayList<>();
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

    /* RETURN ALL THE BLOCKS OF AVAILABILITY */

    // Start at the beginning of the day
    int timeslotStart = TimeRange.START_OF_DAY;

    for (int i = 0; i < mergedEventTimes.size(); i++) {
      // If the gap between timeSlotstart and the start of the next event is >= meeting duration,
      // add to meetingTimes
      TimeRange currEvent = mergedEventTimes.get(i);
      if (currEvent.start() - timeslotStart >= request.getDuration()) {
        TimeRange timeslot = TimeRange.fromStartEnd(timeslotStart, currEvent.start(), false);
        meetingTimes.add(timeslot);
      }
      // the next potential meeting time will start from the end of the current event
      timeslotStart = currEvent.end();
    }

    // Finally, add the timeslot from the end of the last event to the end of the day
    TimeRange lastEvent = mergedEventTimes.get(mergedEventTimes.size() - 1);
    if (TimeRange.END_OF_DAY - lastEvent.end() >= request.getDuration()) {
      TimeRange timeslot = TimeRange.fromStartEnd(lastEvent.end(), TimeRange.END_OF_DAY, true);
      meetingTimes.add(timeslot);
    } 

    return meetingTimes;
  }
}
