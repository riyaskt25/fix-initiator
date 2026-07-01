package com.demo.fix.initiator.mapper;

import org.mapstruct.Mapper;

import com.demo.fix.initiator.message.ExecutionReportXml;
import com.demo.fix.initiator.message.FixMessageFields;

@Mapper(componentModel = "spring")
public interface ExecutionReportXmlMapper extends FixMessageMapper<ExecutionReportXml> {

	@Override
	default boolean supports(String sessionKey, String messageType) {
		return "8".equals(messageType);
	}

	@Override
	ExecutionReportXml map(FixMessageFields fields);
}