package com.demo.fix.initiator.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.demo.fix.initiator.message.FixMessageFields;
import com.demo.fix.initiator.message.NewOrderSingleXml;

@Mapper(componentModel = "spring")
public interface NewOrderSingleXmlMapper extends FixMessageMapper<NewOrderSingleXml> {

	@Override
	default boolean supports(String sessionKey, String messageType) {
		return "D".equals(messageType);
	}

	@Override
	@Mapping(target = "messageType", constant = "D")
	NewOrderSingleXml map(FixMessageFields fields);
}