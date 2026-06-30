package com.demo.fix.initiator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import quickfix.Application;
import quickfix.DefaultMessageFactory;
import quickfix.FieldNotFound;
import quickfix.FileLogFactory;
import quickfix.FileStoreFactory;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.MessageCracker;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.RejectLogon;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.SocketInitiator;
import quickfix.UnsupportedMessageType;
import quickfix.field.ClOrdID;
import quickfix.field.HandlInst;
import quickfix.field.OrdType;
import quickfix.field.OrderQty;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.TransactTime;
import quickfix.fix44.NewOrderSingle;

@Component
public class QuickFixInitiatorService extends MessageCracker implements ApplicationRunner, DisposableBean, Application {

	private static final Logger log = LoggerFactory.getLogger(QuickFixInitiatorService.class);

	private final FixInitiatorProperties properties;
	private final ScheduledExecutorService scheduler;
	private final Map<SessionID, OrderFlow> orderFlows = new ConcurrentHashMap<>();
	private volatile CountDownLatch completionLatch;

	private volatile SocketInitiator initiator;

	public QuickFixInitiatorService(FixInitiatorProperties properties) {
		this.properties = properties;
		ThreadFactory threadFactory = runnable -> {
			Thread thread = new Thread(runnable, "fix-initiator-scheduler");
			thread.setDaemon(false);
			return thread;
		};
		this.scheduler = Executors.newSingleThreadScheduledExecutor(threadFactory);
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		start();
		// Block the Spring application thread so the JVM stays alive while
		// MINA / QFJ threads (which are daemon threads) are running.
		completionLatch.await();
	}

	private synchronized void start() throws Exception {
		if (initiator != null) {
			return;
		}

		List<FixInitiatorProperties.SessionConfig> sessions = properties.getSessions();
		if (sessions == null || sessions.isEmpty()) {
			throw new IllegalStateException("No FIX sessions configured under fix.initiator.sessions");
		}

		Path baseDirectory = Path.of("fix-runtime");
		Path storeDirectory = baseDirectory.resolve("store");
		Path logDirectory = baseDirectory.resolve("log");
		Path settingsFile = baseDirectory.resolve("initiator.cfg");

		deleteDirectory(storeDirectory);
		deleteDirectory(logDirectory);
		Files.createDirectories(storeDirectory);
		Files.createDirectories(logDirectory);
		Files.writeString(settingsFile, buildSettings(storeDirectory, logDirectory), StandardCharsets.UTF_8);

		SessionSettings sessionSettings = new SessionSettings(settingsFile.toString());
		MessageStoreFactory messageStoreFactory = new FileStoreFactory(sessionSettings);
		FileLogFactory logFactory = new FileLogFactory(sessionSettings);
		MessageFactory messageFactory = new DefaultMessageFactory();

		completionLatch = new CountDownLatch(sessions.size());
		initiator = new SocketInitiator(this, messageStoreFactory, sessionSettings, logFactory, messageFactory);
		initiator.start();
		log.info("FIX initiator started with {} session(s)", sessions.size());
	}

	private void deleteDirectory(Path directory) throws IOException {
		if (!Files.exists(directory)) {
			return;
		}
		Files.walkFileTree(directory, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private String buildSettings(Path storeDirectory, Path logDirectory) {
		StringBuilder sb = new StringBuilder();
		sb.append("""
				[DEFAULT]
				ConnectionType=initiator
				HeartBtInt=%d
				ReconnectInterval=%d
				FileStorePath=%s
				FileLogPath=%s
				StartTime=00:00:00
				EndTime=23:59:59
				UseDataDictionary=N
				""".formatted(
				properties.getHeartbeatIntervalSeconds(),
				properties.getReconnectIntervalSeconds(),
				storeDirectory.toAbsolutePath(),
				logDirectory.toAbsolutePath()));

		for (FixInitiatorProperties.SessionConfig session : properties.getSessions()) {
			sb.append("""

					[SESSION]
					BeginString=%s
					SenderCompID=%s
					TargetCompID=%s
					SocketConnectHost=%s
					SocketConnectPort=%d
					""".formatted(
					session.getBeginString(),
					session.getSenderCompId(),
					session.getTargetCompId(),
					session.getHost(),
					session.getPort()));
		}
		return sb.toString();
	}

	@Override
	public void destroy() {
		orderFlows.values().forEach(OrderFlow::cancel);
		orderFlows.clear();

		SocketInitiator currentInitiator = initiator;
		if (currentInitiator != null) {
			currentInitiator.stop();
			initiator = null;
		}

		scheduler.shutdownNow();
		CountDownLatch latch = completionLatch;
		if (latch != null) {
			while (latch.getCount() > 0) {
				latch.countDown();
			}
		}
	}

	@Override
	public void onCreate(SessionID sessionId) {
		log.info("FIX initiator session created: {}", sessionId);
	}

	@Override
	public void onLogon(SessionID sessionId) {
		log.info("FIX initiator logon: {}", sessionId);
		orderFlows.computeIfAbsent(sessionId, id -> startOrderFlow(id, findSessionConfig(id)));
	}

	@Override
	public void onLogout(SessionID sessionId) {
		log.info("FIX initiator logout: {}", sessionId);
		OrderFlow orderFlow = orderFlows.remove(sessionId);
		if (orderFlow != null) {
			orderFlow.cancel();
		}
	}

	@Override
	public void toAdmin(Message message, SessionID sessionId) {
		log.debug("Initiator to admin {}: {}", sessionId, message);
	}

	@Override
	public void fromAdmin(Message message, SessionID sessionId)
			throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
		log.debug("Initiator from admin {}: {}", sessionId, message);
	}

	@Override
	public void toApp(Message message, SessionID sessionId) {
		log.debug("Initiator to app {}: {}", sessionId, message);
	}

	@Override
	public void fromApp(Message message, SessionID sessionId)
			throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
		log.info("Initiator received from {}: {}", sessionId, message);
	}

	private FixInitiatorProperties.SessionConfig findSessionConfig(SessionID sessionId) {
		return properties.getSessions().stream()
				.filter(s -> s.getSenderCompId().equals(sessionId.getSenderCompID())
						&& s.getTargetCompId().equals(sessionId.getTargetCompID()))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException(
						"No session config found for " + sessionId));
	}

	private OrderFlow startOrderFlow(SessionID sessionId, FixInitiatorProperties.SessionConfig config) {
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
				sendOrder(sessionId, currentOrderNumber, config);
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
		return orderFlow;
	}

	private void sendOrder(SessionID sessionId, int currentOrderNumber, FixInitiatorProperties.SessionConfig config) throws Exception {
		String clOrdId = sessionId.getSenderCompID() + "-" + sessionId.getTargetCompID() + "-" + currentOrderNumber;
		NewOrderSingle order = new NewOrderSingle(
				new ClOrdID(clOrdId),
				new Side(resolveSide(config.getSide())),
				new TransactTime(LocalDateTime.now(ZoneId.systemDefault())),
				new OrdType(OrdType.MARKET));
		order.set(new HandlInst(HandlInst.AUTOMATED_EXECUTION_ORDER_PUBLIC_BROKER_INTERVENTION_OK));
		order.set(new Symbol(config.getSymbol()));
		order.set(new OrderQty(config.getQuantity()));

		Session.sendToTarget(order, sessionId);
	}

	private char resolveSide(String configuredSide) {
		if (configuredSide == null) {
			return Side.BUY;
		}
		return switch (configuredSide.trim().toUpperCase()) {
			case "SELL" -> Side.SELL;
			default -> Side.BUY;
		};
	}

	private static final class OrderFlow {

		private final SessionID sessionId;
		private final AtomicInteger sentCount = new AtomicInteger();
		private final AtomicReference<ScheduledFuture<?>> future = new AtomicReference<>();

		private OrderFlow(SessionID sessionId) {
			this.sessionId = sessionId;
		}

		private void cancel() {
			ScheduledFuture<?> scheduledFuture = future.getAndSet(null);
			if (scheduledFuture != null) {
				scheduledFuture.cancel(false);
			}
		}
	}
}
