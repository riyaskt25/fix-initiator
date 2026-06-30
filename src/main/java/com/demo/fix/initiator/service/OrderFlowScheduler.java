package com.demo.fix.initiator.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.demo.fix.initiator.FixInitiatorProperties;
import com.demo.fix.initiator.domain.Order;

import quickfix.SessionID;

/**
 * Manages order flow scheduling per session.
 * Handles periodic order generation and dispatch.
 */
@Service
public class OrderFlowScheduler {

	private static final Logger log = LoggerFactory.getLogger(OrderFlowScheduler.class);

	private final OrderService orderService;
	private final Map<SessionID, OrderFlow> orderFlows = new ConcurrentHashMap<>();

	public OrderFlowScheduler(OrderService orderService) {
		this.orderService = orderService;
	}

	/**
	 * Starts an order flow for a session.
	 * Returns the OrderFlow so callers can track state.
	 */
	public OrderFlow startOrderFlow(
			ScheduledExecutorService scheduler,
			SessionID sessionId,
			FixInitiatorProperties.SessionConfig config,
			CountDownLatch completionLatch) {

		OrderFlow orderFlow = new OrderFlow(sessionId);
		long intervalSeconds = Math.max(1, config.getSendIntervalSeconds());
		long totalRuns = Math.max(1L, (config.getSendDurationMinutes() * 60L) / intervalSeconds);
		AtomicReference<ScheduledFuture<?>> futureReference = new AtomicReference<>();

		ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
			int currentOrderNumber = orderFlow.sentCount.incrementAndGet();
			if (currentOrderNumber > totalRuns) {
				ScheduledFuture<?> scheduledFuture = futureReference.get();
				if (scheduledFuture != null) {
					scheduledFuture.cancel(false);
				}
				orderFlows.remove(sessionId);
				return;
			}

			try {
				Order order = orderService.createOrder(
						new com.demo.fix.initiator.domain.OrderRequest(
								config.getSymbol(),
								config.getSide(),
								config.getQuantity()),
						sessionId.getSenderCompID(),
						sessionId.getTargetCompID());
				orderService.sendOrder(order, sessionId);
				log.info("Initiator sent order {} of {} to {}", currentOrderNumber, totalRuns, sessionId);
			} catch (Exception exception) {
				log.error("Initiator failed to send order {} to {}", currentOrderNumber, sessionId, exception);
			}

			if (currentOrderNumber >= totalRuns) {
				ScheduledFuture<?> scheduledFuture = futureReference.get();
				if (scheduledFuture != null) {
					scheduledFuture.cancel(false);
				}
				orderFlows.remove(sessionId);
				log.info("Initiator order flow complete for session {}, releasing latch", sessionId);
				completionLatch.countDown();
			}
		}, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);

		futureReference.set(future);
		orderFlow.future.set(future);
		orderFlows.put(sessionId, orderFlow);
		return orderFlow;
	}

	/**
	 * Retrieves the current order flow for a session.
	 */
	public OrderFlow getOrderFlow(SessionID sessionId) {
		return orderFlows.get(sessionId);
	}

	/**
	 * Puts an order flow if not already present (similar to computeIfAbsent pattern).
	 */
	public OrderFlow computeIfAbsentOrderFlow(SessionID sessionId, java.util.function.Function<SessionID, OrderFlow> mappingFunction) {
		return orderFlows.computeIfAbsent(sessionId, mappingFunction);
	}

	/**
	 * Cancels all order flows.
	 */
	public void cancelAll() {
		orderFlows.values().forEach(OrderFlow::cancel);
		orderFlows.clear();
	}

	/**
	 * Represents the state of a single session's order flow.
	 */
	public static final class OrderFlow {
		private final SessionID sessionId;
		private final AtomicInteger sentCount = new AtomicInteger();
		private final AtomicReference<ScheduledFuture<?>> future = new AtomicReference<>();

		public OrderFlow(SessionID sessionId) {
			this.sessionId = sessionId;
		}

		public void cancel() {
			ScheduledFuture<?> scheduledFuture = future.getAndSet(null);
			if (scheduledFuture != null) {
				scheduledFuture.cancel(false);
			}
		}

		public int getSentCount() {
			return sentCount.get();
		}

		public SessionID getSessionId() {
			return sessionId;
		}
	}
}
