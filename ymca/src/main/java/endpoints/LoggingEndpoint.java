package endpoints;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.servlet.http.HttpServletRequest;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.json.JSONObject;

/**
 * Endpoint Class used to log messages and send them to the client
 * 
 * @author Jake Lewis
 */
public class LoggingEndpoint extends Endpoint
{

	private static Map<String, Set<LoggingEndpoint>> endpoints = new HashMap<>();
	private static Map<String, Deque<JSONObject>> messages = new HashMap<>();
	private Session session;
	private String sessionId;

	@Override
	public void onOpen(final Session session, final EndpointConfig config)
	{
		this.session = session;
		session.addMessageHandler(new MessageHandler.Whole<String>()
		{
			@Override
			public void onMessage(final String text)
			{
				LoggingEndpoint.this.sessionId = text;
				Set<LoggingEndpoint> set = LoggingEndpoint.endpoints.get(LoggingEndpoint.this.sessionId);
				if (null == set)
				{
					set = new HashSet<>();
					LoggingEndpoint.endpoints.put(LoggingEndpoint.this.sessionId, set);
				}
				set.add(LoggingEndpoint.this);
			}
		});
	}

	/**
	 * Event that is triggered when a session has closed.
	 *
	 * @param session
	 *            The session
	 * @param closeReason
	 *            Why the session was closed
	 */
	@Override
	public void onClose(final Session session, final CloseReason closeReason)
	{
		final Set<LoggingEndpoint> set = LoggingEndpoint.endpoints.get(this.sessionId);
		set.remove(this);
		if (set.isEmpty())
		{
			LoggingEndpoint.endpoints.remove(this.sessionId);
		}
	}

	/**
	 * Logs to all JavaScript Logging Endpoints for all sessions
	 * 
	 * @param level
	 *            The log level e.g. INFO or SEVERE
	 * @param message
	 *            The message to display
	 */
	public static void log(final Level level, final String message)
	{
		LoggingEndpoint.log((String) null, level, message, true);
	}

	/**
	 * Logs to all JavaScript Logging Endpoints for a specific session, logs to
	 * all sessions if ID is <code>null</code>
	 * 
	 * @param request
	 *            The request to get the session to send the message to
	 * @param level
	 *            The log level e.g. INFO or SEVERE
	 * @param message
	 *            The message to display
	 */
	public static void log(final HttpServletRequest request, final Level level, final String message)
	{
		LoggingEndpoint.log(request.getRequestedSessionId(), level, message, true);
	}

	public static void log(final HttpServletRequest request, final Level level, final String message,
	        final boolean queue)
	{
		LoggingEndpoint.log(request.getRequestedSessionId(), level, message, queue);
	}

	public static void log(final String sessionId, final Level level, final String message)
	{
		LoggingEndpoint.log(sessionId, level, message, true);
	}

	/**
	 * Logs to all JavaScript Logging Endpoints for a specific session
	 * 
	 * @param sessionId
	 *            The session to send the message to
	 * @param level
	 *            The log level e.g. INFO or SEVERE
	 * @param message
	 *            The message to display
	 */
	public static void log(final String sessionId, final Level level, final String message, final boolean queue)
	{
		final JSONObject jsonMessage = new JSONObject();
		jsonMessage.put("level", level.getName());
		jsonMessage.put("message", message);

		// If sessionID is not specified, notify all endpoints
		if (null == sessionId)
		{
			for (final Set<LoggingEndpoint> endpoints : LoggingEndpoint.endpoints.values())
			{
				logToClient(endpoints, jsonMessage);
			}

			// Queue message for all sessions
			if (queue)
			{
				for (final String tempSessionId : LoggingEndpoint.endpoints.keySet())
				{
					queueMessage(tempSessionId, jsonMessage);
				}
			}
		}
		else
		{
			final Set<LoggingEndpoint> sessionEndpoints = LoggingEndpoint.endpoints.get(sessionId);
			if (null != sessionEndpoints)
			{
				logToClient(sessionEndpoints, jsonMessage);

				if (queue)
				{
					queueMessage(sessionId, jsonMessage);
				}
			}
			else
			{
				System.err.println(String.format("No LoggingEndpoints regestered for session '%s'", sessionId));
				System.err.println(level.getName() + ": " + message);
			}
		}
	}

	private static void queueMessage(final String sessionId, final JSONObject jsonMessage)
	{
		Deque<JSONObject> messageQueue = LoggingEndpoint.messages.get(sessionId);

		if (null == messageQueue)
		{
			messageQueue = new ArrayDeque<>();
		}

		messageQueue.add(jsonMessage);
		LoggingEndpoint.messages.put(sessionId, messageQueue);
	}

	/**
	 * Returns the queue of existing messages for a given session
	 * 
	 * @param sessionId
	 *            The session ID for the session messages to be retrieved
	 * @return The queue of messages
	 */
	public static Deque<JSONObject> getMessages(final String sessionId)
	{
		return LoggingEndpoint.messages.get(sessionId);
	}

	public static void sendQueuedMessages(final JSONObject messageObject)
	{
		final Set<LoggingEndpoint> sessionEndpoints = LoggingEndpoint.endpoints.get(messageObject.get("sessionId"));

		logToClient(sessionEndpoints, messageObject);
	}

	private static void logToClient(final Set<LoggingEndpoint> endpoints, final JSONObject message)
	{
		if (null != endpoints)
		{
			for (final LoggingEndpoint endpoint : endpoints)
			{
				try
				{
					endpoint.session.getBasicRemote().sendText(message.toString());
				}
				catch (final IOException e)
				{
					e.printStackTrace();
				}
			}
		}
	}
}