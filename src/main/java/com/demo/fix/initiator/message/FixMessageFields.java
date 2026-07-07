package com.demo.fix.initiator.message;

/**
 * Canonical FIX payload extracted from a QuickFIX/J message.
 * Used as the source object for MapStruct mappings to XML DTOs.
 */
public record FixMessageFields(
		String sessionKey,
		String beginString,
		String bodyLength,
		String msgSeqNum,
		String senderCompId,
		String targetCompId,
		String messageType,
		String sendingTime,
		String rawFix,
		String clOrdId,
		String orderId,
		String execId,
		String symbol,
		String side,
		String quantity,
		String ordStatus,
		String text,
		String transactTime) {
}