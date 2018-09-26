package org.apereo.openequella.integration.blackboard.common;

import blackboard.platform.log.LogService;
import blackboard.platform.log.LogServiceFactory;

// Wraps the common logger, and exposes the ability to soft 'change'
// Logging Levels
public class BbLogger {

	private static final String SIG = "oeqInteg - ";
	private static final LogService LOGGER = LogServiceFactory.getInstance();

	// Maybe not the best way to handle this.
	private static BbLogger instance;
	private static final Object instanceLock = new Object();

	private LogLevel logLevel = LogLevel.NotSet;

	// If NotSet is chosen, the logger will use the standard logger
	// levels for messaging. If any other value is chosen, the logger
	// will still honor the log levels, filtered by the level set.
	public enum LogLevel {
		NotSet, Warn, Info, Debug, SqlTrace, Trace,
	}

	@SuppressWarnings("null")
	public static BbLogger instance() {
		if (instance == null) {
			synchronized (instanceLock) {
				if (instance == null) {
					instance = new BbLogger();
				}
			}
		}
		return instance;
	}

	private BbLogger() {

	}

	public synchronized void setLoggingLevel(LogLevel level) {
		logLevel = level;
	}

	public void logTrace(String msg) {
		if (logLevel == LogLevel.NotSet) {
			LOGGER.logAudit(SIG + msg);
		} else if (logLevel == LogLevel.Trace) {
			LOGGER.logWarning(SIG + "Trace - " + msg);
		}
	}

	public void logSqlTrace(String msg) {
		if (logLevel == LogLevel.NotSet) {
			LOGGER.logAudit(SIG + msg);
		} else if ((logLevel == LogLevel.SqlTrace) || (logLevel == LogLevel.Trace)) {
			LOGGER.logWarning(SIG + "SqlTrace - " + msg);
		}
	}

	public void logDebug(String msg) {
		if (logLevel == LogLevel.NotSet) {
			LOGGER.logDebug(SIG + msg);
		} else if ((logLevel == LogLevel.Debug) || (logLevel == LogLevel.SqlTrace) || (logLevel == LogLevel.Trace)) {
			LOGGER.logWarning(SIG + "Debug - " + msg);
		}
	}

	public void logInfo(String msg) {
		if (logLevel == LogLevel.NotSet) {
			LOGGER.logInfo(SIG + msg);
		} else if ((logLevel == LogLevel.Info) || (logLevel == LogLevel.Debug) || (logLevel == LogLevel.SqlTrace)
				|| (logLevel == LogLevel.Trace)) {
			LOGGER.logWarning(SIG + "Info - " + msg);
		}
	}

	public void logWarn(String msg) {
		LOGGER.logWarning(SIG + msg);
	}

	public void logError(String msg) {
		LOGGER.logError(SIG + msg);
	}

	public void logError(String msg, Exception t) {
		LOGGER.logError(SIG + msg, t);
	}

	public static String getLoggingDetails() {
		return "LogFileName=[" + LOGGER.getLogFileName() + "], LogName=[" + LOGGER.getLogName() + "], LogVerbosity=["
				+ LOGGER.getVerbosityLevel().toExternalString() + "]";
	}
}
