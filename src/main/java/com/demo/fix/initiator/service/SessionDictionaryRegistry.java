package com.demo.fix.initiator.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import quickfix.DataDictionary;
import quickfix.SessionID;

/**
 * Holds a per-session DataDictionary loaded from the paths declared in initiator.cfg.
 * Populated by QuickFixInitiatorService during startup, after dictionary files are
 * extracted to disk. Consulted by GenericFixXmlSerializer when serializing messages.
 */
@Component
public class SessionDictionaryRegistry {

	private final Map<SessionID, DataDictionary> dictionaries = new ConcurrentHashMap<>();

	public void register(SessionID sessionId, DataDictionary dictionary) {
		dictionaries.put(sessionId, dictionary);
	}

	/** Returns the DataDictionary for the given session, or null if not registered. */
	public DataDictionary get(SessionID sessionId) {
		return dictionaries.get(sessionId);
	}
}
