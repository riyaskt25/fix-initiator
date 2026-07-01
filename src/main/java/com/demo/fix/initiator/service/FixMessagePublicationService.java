package com.demo.fix.initiator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.demo.fix.initiator.mapper.FixMessageMapper;
import com.demo.fix.initiator.mapper.UnknownFixMessageXmlMapper;
import com.demo.fix.initiator.message.FixMessageFields;

import quickfix.FieldNotFound;
import quickfix.Message;
import quickfix.SessionID;

@Service
public class FixMessagePublicationService {

	private static final Logger log = LoggerFactory.getLogger(FixMessagePublicationService.class);

	private final FixMessageFieldExtractor fieldExtractor;
	private final FixMessageMapperRegistry mapperRegistry;
	private final UnknownFixMessageXmlMapper fallbackMapper;
	private final DummyXmlBrokerPublisher dummyXmlBrokerPublisher;

	public FixMessagePublicationService(
			FixMessageFieldExtractor fieldExtractor,
			FixMessageMapperRegistry mapperRegistry,
			UnknownFixMessageXmlMapper fallbackMapper,
			DummyXmlBrokerPublisher dummyXmlBrokerPublisher) {
		this.fieldExtractor = fieldExtractor;
		this.mapperRegistry = mapperRegistry;
		this.fallbackMapper = fallbackMapper;
		this.dummyXmlBrokerPublisher = dummyXmlBrokerPublisher;
	}

	public void publish(Message message, SessionID sessionId) throws FieldNotFound {
		FixMessageFields fields = fieldExtractor.extract(message, sessionId);
		FixMessageMapper<?> mapper = mapperRegistry.resolve(fields.sessionKey(), fields.messageType())
				.orElse(fallbackMapper);
		Object payload = mapper.map(fields);
		if (mapper == fallbackMapper) {
			log.info("No dedicated mapper found for session={} type={}, using fallback XML contract", fields.sessionKey(), fields.messageType());
		}
		dummyXmlBrokerPublisher.publish(fields, payload);
	}
}