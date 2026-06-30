package com.demo.fix.initiator.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.demo.fix.initiator.domain.Order;
import com.demo.fix.initiator.domain.OrderRequest;

import quickfix.Session;
import quickfix.SessionID;
import quickfix.field.ClOrdID;
import quickfix.field.HandlInst;
import quickfix.field.OrdType;
import quickfix.field.OrderQty;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.TransactTime;
import quickfix.fix44.NewOrderSingle;

/**
 * Service for creating and sending FIX orders.
 * Encapsulates order creation logic and decouples it from scheduling/session management.
 */
@Service
public class OrderService {

	private static final Logger log = LoggerFactory.getLogger(OrderService.class);

	/**
	 * Creates an Order domain object from a request with a unique ClOrdID.
	 * Uses UUID to ensure uniqueness across restarts and across all sessions.
	 */
	public Order createOrder(
			OrderRequest orderRequest,
			String senderCompId,
			String targetCompId) {
		// Generate unique ClOrdID using UUID
		String clOrdId = senderCompId + "-" + targetCompId + "-" + UUID.randomUUID().toString();
		
		return new Order(
				clOrdId,
				orderRequest.getSymbol(),
				resolveSide(orderRequest.getSide()),
				orderRequest.getQuantity(),
				LocalDateTime.now(ZoneId.systemDefault()),
				senderCompId,
				targetCompId
		);
	}

	/**
	 * Sends an Order to the target session via QFJ.
	 */
	public void sendOrder(Order order, SessionID sessionId) throws Exception {
		NewOrderSingle fixOrder = buildFixOrder(order);
		Session.sendToTarget(fixOrder, sessionId);
		log.info("Order sent: {} to session {}", order.clOrdId(), sessionId);
	}

	/**
	 * Builds a FIX NewOrderSingle message from a domain Order.
	 */
	private NewOrderSingle buildFixOrder(Order order) {
		NewOrderSingle fixOrder = new NewOrderSingle(
				new ClOrdID(order.clOrdId()),
				new Side(order.side()),
				new TransactTime(order.transactTime()),
				new OrdType(OrdType.MARKET));
		fixOrder.set(new HandlInst(HandlInst.AUTOMATED_EXECUTION_ORDER_PUBLIC_BROKER_INTERVENTION_OK));
		fixOrder.set(new Symbol(order.symbol()));
		fixOrder.set(new OrderQty(order.quantity()));
		return fixOrder;
	}

	/**
	 * Converts string side (BUY/SELL) to FIX char.
	 */
	private char resolveSide(String configuredSide) {
		if (configuredSide == null) {
			return Side.BUY;
		}
		return switch (configuredSide.trim().toUpperCase()) {
			case "SELL" -> Side.SELL;
			default -> Side.BUY;
		};
	}
}
