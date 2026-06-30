package com.demo.fix.initiator.domain;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request object for creating orders from configuration.
 * Includes validation for order parameters.
 */
public class OrderRequest {

	@NotBlank(message = "symbol cannot be blank")
	private String symbol;

	@Pattern(regexp = "^(BUY|SELL)$", message = "side must be BUY or SELL")
	private String side;

	@Min(value = 1, message = "quantity must be at least 1")
	private int quantity;

	public OrderRequest(String symbol, String side, int quantity) {
		this.symbol = symbol;
		this.side = side;
		this.quantity = quantity;
	}

	public String getSymbol() {
		return symbol;
	}

	public String getSide() {
		return side;
	}

	public int getQuantity() {
		return quantity;
	}

	@Override
	public String toString() {
		return "OrderRequest{" +
				"symbol='" + symbol + '\'' +
				", side='" + side + '\'' +
				", quantity=" + quantity +
				'}';
	}
}
