package com.demo.fix.initiator.domain;

import java.time.LocalDateTime;

/**
 * Immutable domain object representing a FIX order.
 */
public record Order(
	String clOrdId,
	String symbol,
	char side,
	int quantity,
	LocalDateTime transactTime,
	String senderCompId,
	String targetCompId
) {
	public Order {
		if (clOrdId == null || clOrdId.isBlank()) {
			throw new IllegalArgumentException("clOrdId cannot be blank");
		}
		if (symbol == null || symbol.isBlank()) {
			throw new IllegalArgumentException("symbol cannot be blank");
		}
		if (quantity <= 0) {
			throw new IllegalArgumentException("quantity must be positive");
		}
		if (transactTime == null) {
			throw new IllegalArgumentException("transactTime cannot be null");
		}
		if (senderCompId == null || senderCompId.isBlank()) {
			throw new IllegalArgumentException("senderCompId cannot be blank");
		}
		if (targetCompId == null || targetCompId.isBlank()) {
			throw new IllegalArgumentException("targetCompId cannot be blank");
		}
	}
}
