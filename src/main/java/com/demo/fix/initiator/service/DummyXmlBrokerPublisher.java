package com.demo.fix.initiator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.demo.fix.initiator.service.xml.GenericFixXmlSerializer;

import quickfix.FieldNotFound;
import quickfix.Message;
import quickfix.SessionID;

@Service
public class DummyXmlBrokerPublisher {

	private static final Logger log = LoggerFactory.getLogger(DummyXmlBrokerPublisher.class);

	private final GenericFixXmlSerializer xmlSerializer;

	public DummyXmlBrokerPublisher(GenericFixXmlSerializer xmlSerializer) {
		this.xmlSerializer = xmlSerializer;
	}

	public void publish(SessionID sessionId, Message message) {
		String messageType = readHeaderField(message, 35);
		String xml = xmlSerializer.serialize(message);
		log.info("Dummy MQ publish session={} type={} xml=\n{}", sessionId, messageType, xml);
	}

	private String readHeaderField(Message message, int tag) {
		if (!message.getHeader().isSetField(tag)) {
			return "UNKNOWN";
		}
		try {
			return message.getHeader().getString(tag);
		} catch (FieldNotFound e) {
			return "UNKNOWN";
		}
	}
}