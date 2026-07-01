package com.demo.fix.initiator.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "FixMessage")
public record UnknownFixMessageXml(
		String sessionKey,
		String beginString,
		String senderCompId,
		String targetCompId,
		String messageType,
		String clOrdId,
		String orderId,
		String execId,
		String symbol,
		String side,
		String quantity,
		String ordStatus,
		String text,
		String transactTime,
		String rawFix) {
}