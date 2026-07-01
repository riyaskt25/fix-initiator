package com.demo.fix.initiator.mapper;

import org.mapstruct.Mapper;

import com.demo.fix.initiator.message.FixMessageFields;
import com.demo.fix.initiator.message.UnknownFixMessageXml;

@Mapper(componentModel = "spring")
public interface UnknownFixMessageXmlMapper extends FixMessageMapper<UnknownFixMessageXml> {

	@Override
	default boolean supports(String sessionKey, String messageType) {
		return false;
	}

	@Override
	UnknownFixMessageXml map(FixMessageFields fields);
}