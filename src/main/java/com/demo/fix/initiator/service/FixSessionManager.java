package com.demo.fix.initiator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import quickfix.Application;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.MessageCracker;
import quickfix.RejectLogon;
import quickfix.SessionID;
import quickfix.UnsupportedMessageType;

/**
 * Handles FIX protocol callbacks and session lifecycle.
 * Implements the QFJ Application interface for message routing.
 * Receives orders from the acceptor and processes them.
 */
@Service
public class FixSessionManager extends MessageCracker implements Application {

	private static final Logger log = LoggerFactory.getLogger(FixSessionManager.class);
	private final FixMessagePublicationService publicationService;

	public FixSessionManager(FixMessagePublicationService publicationService) {
		this.publicationService = publicationService;
	}

	@Override
	public void onCreate(SessionID sessionId) {
		log.info("FIX session created: {}", sessionId);
	}

	@Override
	public void onLogon(SessionID sessionId) {
		log.info("FIX logon successful: {}", sessionId);
	}

	@Override
	public void onLogout(SessionID sessionId) {
		log.info("FIX logout: {}", sessionId);
	}

	@Override
	public void toAdmin(Message message, SessionID sessionId) {
		log.debug("To admin {}: {}", sessionId, message);
	}

	@Override
	public void fromAdmin(Message message, SessionID sessionId)
			throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
		log.debug("From admin {}: {}", sessionId, message);
	}

	@Override
	public void toApp(Message message, SessionID sessionId) {
		log.debug("To app {}: {}", sessionId, message);
	}

	@Override
	public void fromApp(Message message, SessionID sessionId)
			throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
		log.info("Received order from {}: {}", sessionId, message);
		publicationService.publish(message, sessionId);
	}
}
