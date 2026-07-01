package com.demo.fix.initiator.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "ExecutionReport")
public record ExecutionReportXml(
		String sessionKey,
		String beginString,
		String senderCompId,
		String targetCompId,
		String messageType,
		String orderId,
		String execId,
		String clOrdId,
		String ordStatus,
		String text,
		String transactTime,
		String rawFix) {
}