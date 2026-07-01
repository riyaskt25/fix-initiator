package com.demo.fix.initiator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;

import com.demo.fix.initiator.message.FixMessageFields;

@Service
public class DummyXmlBrokerPublisher {

	private static final Logger log = LoggerFactory.getLogger(DummyXmlBrokerPublisher.class);

	private final XmlMessageSerializer xmlMessageSerializer;

	public DummyXmlBrokerPublisher(XmlMessageSerializer xmlMessageSerializer) {
		this.xmlMessageSerializer = xmlMessageSerializer;
	}

	public void publish(FixMessageFields fields, Object payload) {
		try {
			String xml = xmlMessageSerializer.toXml(payload);
			log.info("Dummy MQ publish session={} type={} xml=\n{}", fields.sessionKey(), fields.messageType(), xml);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Failed to serialize FIX payload to XML", e);
		}
	}
}