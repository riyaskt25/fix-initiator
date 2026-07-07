package com.demo.fix.initiator.service;

import org.springframework.stereotype.Service;

import quickfix.Message;
import quickfix.SessionID;

@Service
public class FixMessagePublicationService {

	private final DummyXmlBrokerPublisher dummyXmlBrokerPublisher;

	public FixMessagePublicationService(
			DummyXmlBrokerPublisher dummyXmlBrokerPublisher) {
		this.dummyXmlBrokerPublisher = dummyXmlBrokerPublisher;
	}

	public void publish(Message message, SessionID sessionId) {
		dummyXmlBrokerPublisher.publish(sessionId, message);
	}
}