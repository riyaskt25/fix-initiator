package com.demo.fix.initiator.service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.demo.fix.initiator.FixInitiatorProperties;

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
 * Decoupled from startup orchestration and order flow scheduling.
 */
@Service
public class FixSessionManager extends MessageCracker implements Application {

	private static final Logger log = LoggerFactory.getLogger(FixSessionManager.class);

	private final OrderFlowScheduler orderFlowScheduler;
	private FixInitiatorProperties properties;
	private ScheduledExecutorService scheduler;
	private CountDownLatch completionLatch;

	public FixSessionManager(OrderFlowScheduler orderFlowScheduler) {
		this.orderFlowScheduler = orderFlowScheduler;
	}

	/**
	 * Initializes the manager with runtime context.
	 */
	public void initialize(FixInitiatorProperties properties, ScheduledExecutorService scheduler,
			CountDownLatch completionLatch) {
		this.properties = properties;
		this.scheduler = scheduler;
		this.completionLatch = completionLatch;
	}

	@Override
	public void onCreate(SessionID sessionId) {
		log.info("FIX session created: {}", sessionId);
	}

	@Override
	public void onLogon(SessionID sessionId) {
		log.info("FIX logon: {}", sessionId);
		// Start order flow for this session
		orderFlowScheduler.computeIfAbsentOrderFlow(sessionId, id -> 
			orderFlowScheduler.startOrderFlow(scheduler, id, findSessionConfig(id), completionLatch));
	}

	@Override
	public void onLogout(SessionID sessionId) {
		log.info("FIX logout: {}", sessionId);
		OrderFlowScheduler.OrderFlow orderFlow = orderFlowScheduler.getOrderFlow(sessionId);
		if (orderFlow != null) {
			orderFlow.cancel();
		}
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
		log.info("Received from {}: {}", sessionId, message);
	}

	private FixInitiatorProperties.SessionConfig findSessionConfig(SessionID sessionId) {
		return properties.getSessions().stream()
				.filter(s -> s.getSenderCompId().equals(sessionId.getSenderCompID())
						&& s.getTargetCompId().equals(sessionId.getTargetCompID()))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException(
						"No session config found for " + sessionId));
	}
}
