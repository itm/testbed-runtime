/* Copyright (c) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package de.uniluebeck.itm.tr.rs.persistence.gcal;

import com.google.gdata.client.Query;
import com.google.gdata.client.calendar.CalendarQuery;
import com.google.gdata.client.calendar.CalendarService;
import com.google.gdata.data.DateTime;
import com.google.gdata.data.Link;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.batch.BatchOperationType;
import com.google.gdata.data.batch.BatchStatus;
import com.google.gdata.data.batch.BatchUtils;
import com.google.gdata.data.calendar.CalendarEntry;
import com.google.gdata.data.calendar.CalendarEventEntry;
import com.google.gdata.data.calendar.CalendarEventFeed;
import com.google.gdata.data.calendar.CalendarFeed;
import com.google.gdata.data.extensions.Reminder;
import com.google.gdata.data.extensions.Reminder.Method;
import com.google.gdata.data.extensions.When;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

/**
 * ITM Google Testbed ReservationData Calendar
 */
public class ITMGoogleCalendar {

	private static ITMGoogleCalendar myCal;

	// The base URL for a user's calendar metafeed (needs a username appended).
	private final String METAFEED_URL_BASE = "http://www.google.com/calendar/feeds/";

	// The string to add to the user's metafeedUrl to access the event feed for
	// their primary calendar.
	private String EVENT_FEED_URL_SUFFIX = "/private/full";

	private String userName = "testbed-runtime-unittests@itm.uni-luebeck.de";

	private String userPassword = "testbed-runtime-unittests123";

	// The URL for the metafeed of the specified user.
	// (e.g. http://www.google.com/feeds/calendar/jdoe@gmail.com)
	private URL metafeedUrl = null;

	// The URL for the event feed of the specified user's primary calendar.
	// (e.g. http://www.googe.com/feeds/calendar/jdoe@gmail.com/private/full)
	private URL eventFeedUrl = null;

	private CalendarService myService;


	public ITMGoogleCalendar() {

		myService = new CalendarService("WISEBED-CalenderService");

		try {
			metafeedUrl = new URL(METAFEED_URL_BASE + userName);
			eventFeedUrl = new URL(METAFEED_URL_BASE + userName + EVENT_FEED_URL_SUFFIX);
		} catch (MalformedURLException e) {
			System.err.println("System says invalid URL.");
			e.printStackTrace();
			return;
		}

		try {
			myService.setUserCredentials(userName, userPassword);
		} catch (AuthenticationException e) {
			System.err.println("System doesn't accept username and password.");
			e.printStackTrace();
		}
	}


	/**
	 * Prints a list of all the user's calendars.
	 *
	 * @throws ServiceException
	 * 		If the service is unable to handle the request.
	 * @throws IOException
	 * 		Error communicating with the server
	 */
	public void printUserCalendars() throws IOException, ServiceException {
		// Send the request and receive the response:
		CalendarFeed resultFeed = myService.getFeed(metafeedUrl, CalendarFeed.class);
		System.out.println("Your calendars:");
		System.out.println();
		for (int i = 0; i < resultFeed.getEntries().size(); i++) {
			CalendarEntry entry = resultFeed.getEntries().get(i);
			System.out.println("\t " + entry.getTitle().getPlainText() + " " + entry.getSelfLink().getHref());
		}
		System.out.println();
	}

	/**
	 * Prints the titles of all experiments on the calendar
	 *
	 * @throws ServiceException
	 * 		If the service is unable to handle the request.
	 * @throws IOException
	 * 		Error communicating with the server.
	 */
	public void printAllReservations() throws ServiceException, IOException {
		// Send the request and receive the response:
		CalendarEventFeed resultFeed = myService.getFeed(eventFeedUrl,
				CalendarEventFeed.class
		);

		System.out.println("All events on your calendar:");
		System.out.println();
		for (int i = 0; i < resultFeed.getEntries().size(); i++) {
			CalendarEventEntry entry = resultFeed.getEntries().get(i);
			System.out.println(
					"\t" + entry.getTitle().getPlainText() + " " + entry.getPlainTextContent() + " " + entry.getTimes()
							.get(0).getStartTime().toUiString() + " " + entry.getTimes().get(0).getEndTime()
							.toUiString()
			);
		}
		System.out.println();
	}

	/**
	 * Returns a List of all experiments
	 *
	 * @return List of experiments
	 *
	 * @throws ServiceException
	 * 		If the service is unable to handle the request.
	 * @throws IOException
	 * 		Error communicating with the server.
	 */
	public CalendarEventFeed getAllReservation() throws ServiceException, IOException {
		// Send the request and receive the response:
		CalendarEventFeed resultFeed = myService.getFeed(eventFeedUrl, CalendarEventFeed.class);

		System.out.println("All events on your calendar:");
		System.out.println();
		for (int i = 0; i < resultFeed.getEntries().size(); i++) {
			CalendarEventEntry entry = resultFeed.getEntries().get(i);
			System.out.println(
					"\t" + entry.getTitle().getPlainText() + " {" + entry.getPlainTextContent() + "} " + entry
							.getTimes().get(0).getStartTime().toUiString() + " " + entry.getTimes().get(0).getEndTime()
							.toUiString()
			);
		}
		System.out.println();
		return resultFeed;
	}

	/**
	 * Returns a list of all experiments matching the query.
	 *
	 * @param query
	 * 		The text for which to query.
	 *
	 * @return List of experiments
	 *
	 * @throws ServiceException
	 * 		If the service is unable to handle the request.
	 * @throws IOException
	 * 		Error communicating with the server.
	 */
	public CalendarEventFeed getReservationsbyTitle(String query) throws ServiceException, IOException {
		Query myQuery = new Query(eventFeedUrl);
		myQuery.setFullTextQuery(query);

		CalendarEventFeed resultFeed = myService.query(myQuery, CalendarEventFeed.class);

		System.out.println("Events matching " + query + ":");
		System.out.println();
		for (int i = 0; i < resultFeed.getEntries().size(); i++) {
			CalendarEventEntry entry = resultFeed.getEntries().get(i);
			System.out.println(
					"\t" + entry.getTitle().getPlainText() + " {" + entry.getPlainTextContent() + "} " + entry
							.getTimes().get(0).getStartTime().toUiString() + " " + entry.getTimes().get(0).getEndTime()
							.toUiString()
			);
		}
		System.out.println();
		return resultFeed;
	}

	/**
	 * Returns a list of all experiments in a specified date/time range.
	 *
	 * @param startTime
	 * 		Start time (inclusive) of events to print.
	 * @param endTime
	 * 		End time (exclusive) of events to print.
	 *
	 * @return List of experiments
	 *
	 * @throws ServiceException
	 * 		If the service is unable to handle the request.
	 * @throws IOException
	 * 		Error communicating with the server.
	 */
	public CalendarEventFeed getReservationsbyDateRange(DateTime startTime, DateTime endTime)
			throws ServiceException, IOException {
		CalendarQuery myQuery = new CalendarQuery(eventFeedUrl);
		myQuery.setMinimumStartTime(startTime);
		myQuery.setMaximumStartTime(endTime);

		// Send the request and receive the response:
		CalendarEventFeed resultFeed = myService.query(myQuery, CalendarEventFeed.class);

		System.out.println("Events from " + startTime.toUiString() + " to " + endTime.toUiString() + ":");
		System.out.println();
		for (int i = 0; i < resultFeed.getEntries().size(); i++) {
			CalendarEventEntry entry = resultFeed.getEntries().get(i);
			System.out.println(
					"\t" + entry.getTitle().getPlainText() + " {" + entry.getPlainTextContent() + "} " + entry
							.getTimes().get(0).getStartTime().toUiString() + " " + entry.getTimes().get(0).getEndTime()
							.toUiString()
			);
		}
		System.out.println();
		return resultFeed;
	}

	public static DateTime newDateTime(int year, int month, int date, int hourOfDay, int minute) {
		Calendar calendar = new GregorianCalendar();
		calendar.set(year, month - 1, date, hourOfDay, minute);
		return new DateTime(calendar.getTime(), TimeZone.getDefault());
	}

	/**
	 * Creates a new Experiment Entry in the Main calendar
	 *
	 * @param eventTitle
	 * 		Title of the event to create.
	 * @param nodeList
	 * 		Text content of the event - List of reserved nodes.
	 * @param startTime
	 * 		Start of the Experiment
	 * @param endTime
	 * 		End of the Experiment
	 *
	 * @return The newly-created CalendarEventEntry.
	 *
	 * @throws ServiceException
	 * 		If the service is unable to handle the request.
	 * @throws IOException
	 * 		Error communicating with the server.
	 */
	public CalendarEventEntry createReservation(String eventTitle, String nodeList, DateTime startTime,
												DateTime endTime) throws ServiceException, IOException {
		CalendarEventEntry myEntry = new CalendarEventEntry();

		myEntry.setTitle(new PlainTextConstruct(eventTitle));
		myEntry.setContent(new PlainTextConstruct(nodeList));
		myEntry.setQuickAdd(false);
		myEntry.setWebContent(null);

		When eventTimes = new When();
		eventTimes.setStartTime(startTime);
		eventTimes.setEndTime(endTime);
		myEntry.addTime(eventTimes);


		// Send the request and receive the response:

		return myService.insert(eventFeedUrl, myEntry);
	}

	/**
	 * Updates the title of an existing calendar event.
	 *
	 * @param entry
	 * 		The event to update.
	 * @param newTitle
	 * 		The new title for this event.
	 *
	 * @return The updated CalendarEventEntry object.
	 *
	 * @throws ServiceException
	 * 		If the service is unable to handle the request.
	 * @throws IOException
	 * 		Error communicating with the server.
	 */
	public CalendarEventEntry updateTitle(CalendarEventEntry entry, String newTitle)
			throws ServiceException, IOException {
		entry.setTitle(new PlainTextConstruct(newTitle));
		return entry.update();
	}

	/**
	 * Updates the title of an existing calendar event.
	 *
	 * @param entry
	 * 		The event to update.
	 * @param newTitle
	 * 		The new title for this event.
	 *
	 * @return The updated CalendarEventEntry object.
	 *
	 * @throws ServiceException
	 * 		If the service is unable to handle the request.
	 * @throws IOException
	 * 		Error communicating with the server.
	 */
	public CalendarEventEntry updateListofNodes(CalendarEventEntry entry, String newTitle)
			throws ServiceException, IOException {
		entry.setTitle(new PlainTextConstruct(newTitle));
		return entry.update();
	}

	/**
	 * Deletes the event.
	 *
	 * @param event
	 * 		The event to delete.
	 *
	 * @throws ServiceException
	 * 		If the service is unable to handle the request.
	 * @throws IOException
	 * 		Error communicating with the server.
	 */
	public void deleteReservation(CalendarEventEntry event) throws ServiceException, IOException {
		event.delete();
	}

	/**
	 * Adds a reminder to a calendar event.
	 *
	 * @param entry
	 * 		The event to update.
	 * @param numMinutes
	 * 		Reminder time, in minutes.
	 * @param methodType
	 * 		Method of notification (e.g. email, alert, sms).
	 *
	 * @return The updated EventEntry object.
	 *
	 * @throws ServiceException
	 * 		If the service is unable to handle the request.
	 * @throws IOException
	 * 		Error communicating with the server.
	 */
	public CalendarEventEntry addReminder(CalendarEventEntry entry, int numMinutes, Method methodType)
			throws ServiceException, IOException {
		Reminder reminder = new Reminder();
		reminder.setMinutes(numMinutes);
		reminder.setMethod(methodType);
		entry.getReminder().add(reminder);

		return entry.update();
	}

	/**
	 * Makes a batch request to delete all the events in the given list. If any of
	 * the operations fails, the errors returned from the server are displayed.
	 * The CalendarEntry objects in the list given as a parameters must be entries
	 * returned from the server that contain valid edit links (for optimistic
	 * concurrency to work).
	 *
	 * @param eventsToDelete
	 * 		A list of CalendarEventEntry objects to delete.
	 *
	 * @throws ServiceException
	 * 		If the service is unable to handle the request.
	 * @throws IOException
	 * 		Error communicating with the server.
	 */
	public void deleteReservation(List<CalendarEventEntry> eventsToDelete) throws ServiceException, IOException {

		// Add each item in eventsToDelete to the batch request.
		CalendarEventFeed batchRequest = new CalendarEventFeed();
		for (int i = 0; i < eventsToDelete.size(); i++) {
			CalendarEventEntry toDelete = eventsToDelete.get(i);
			// Modify the entry toDelete with batch ID and operation type.
			BatchUtils.setBatchId(toDelete, String.valueOf(i));
			BatchUtils.setBatchOperationType(toDelete, BatchOperationType.DELETE);
			batchRequest.getEntries().add(toDelete);
		}

		// Get the URL to make batch requests to
		CalendarEventFeed feed = myService.getFeed(eventFeedUrl, CalendarEventFeed.class);

		Link batchLink = feed.getLink(Link.Rel.FEED_BATCH, Link.Type.ATOM);
		URL batchUrl = new URL(batchLink.getHref());

		// Submit the batch request
		CalendarEventFeed batchResponse = myService.batch(batchUrl, batchRequest);

		// Ensure that all the operations were successful.
		boolean isSuccess = true;
		for (CalendarEventEntry entry : batchResponse.getEntries()) {
			String batchId = BatchUtils.getBatchId(entry);
			if (!BatchUtils.isSuccess(entry)) {
				isSuccess = false;
				BatchStatus status = BatchUtils.getBatchStatus(entry);
				System.out.println("\n" + batchId + " failed (" + status.getReason()
						+ ") " + status.getContent()
				);
			}
		}
		if (isSuccess) {
			System.out.println("Successfully deleted all events via batch request.");
		}
	}


	public static void main(String[] args) {

		myCal = new ITMGoogleCalendar();

		try {

			// Demonstrate retrieving a list of the user's calendars.
			//myCal.printUserCalendars();

			// Demonstrate creating a single-occurrence event.
			DateTime startDate = newDateTime(2010, 03, 04, 21, 15);
			DateTime endDate = newDateTime(2010, 03, 04, 23, 15);

			CalendarEventEntry event = myCal.createReservation("Lost", "Neue Folge.", startDate, endDate);
			System.out.println("Successfully created event " + event.getTitle().getPlainText());

			// Demonstrate various feed queries.
			myCal.printAllReservations();

			DateTime startTime = newDateTime(2010, 03, 01, 21, 15);
			DateTime endTime = newDateTime(2010, 03, 14, 23, 15);

			myCal.getReservationsbyDateRange(startTime, endTime);

			myCal.deleteReservation(myCal.getReservationsbyTitle("Lost").getEntries().get(0));

			myCal.printAllReservations();


		} catch (IOException e) {
			// Communications error
			System.err.println("There was a problem communicating with the service.");
			e.printStackTrace();
		} catch (ServiceException e) {
			// Server side error
			System.err.println("The server had a problem handling your request.");
			e.printStackTrace();
		}
	}
}
