package com.demo.fix.initiator.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "NewOrderSingle")
public record NewOrderSingleXml(
		String sessionKey,
		String beginString,
		String senderCompId,
		String targetCompId,
		String messageType,
		String clOrdId,
		String symbol,
		String side,
		String quantity,
		String transactTime,
		String rawFix) {
}