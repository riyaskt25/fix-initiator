package com.demo.fix.initiator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.demo.fix.initiator.service.FixSessionManager;
import com.demo.fix.initiator.service.FixSettingsBuilder;

import quickfix.DefaultMessageFactory;
import quickfix.FileLogFactory;
import quickfix.FileStoreFactory;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.SessionSettings;
import quickfix.SocketInitiator;

/**
 * Orchestrates FIX initiator startup and shutdown.
 * Delegates specific responsibilities to dedicated service classes:
 * - FixSessionManager: FIX protocol callbacks
 * - FixSettingsBuilder: Configuration file generation
 * - OrderFlowScheduler: Order flow management
 * - OrderService: Order creation and sending
 */
@Component
public class QuickFixInitiatorService implements ApplicationRunner, DisposableBean {

	private static final Logger log = LoggerFactory.getLogger(QuickFixInitiatorService.class);

	private final FixInitiatorProperties properties;
	private final FixSessionManager fixSessionManager;
	private final FixSettingsBuilder settingsBuilder;
	private final ScheduledExecutorService scheduler;

	private volatile SocketInitiator initiator;
	private volatile CountDownLatch completionLatch;

	public QuickFixInitiatorService(
			FixInitiatorProperties properties,
			FixSessionManager fixSessionManager,
			FixSettingsBuilder settingsBuilder) {
		this.properties = properties;
		this.fixSessionManager = fixSessionManager;
		this.settingsBuilder = settingsBuilder;
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

		// Build settings using the dedicated builder
		String settings = settingsBuilder.buildSettings(
				properties.getHeartbeatIntervalSeconds(),
				properties.getReconnectIntervalSeconds(),
				storeDirectory.toAbsolutePath().toString(),
				logDirectory.toAbsolutePath().toString(),
				sessions);
		Files.writeString(settingsFile, settings, StandardCharsets.UTF_8);

		SessionSettings sessionSettings = new SessionSettings(settingsFile.toString());
		MessageStoreFactory messageStoreFactory = new FileStoreFactory(sessionSettings);
		FileLogFactory logFactory = new FileLogFactory(sessionSettings);
		MessageFactory messageFactory = new DefaultMessageFactory();

		completionLatch = new CountDownLatch(sessions.size());
		fixSessionManager.initialize(properties, scheduler, completionLatch);

		initiator = new SocketInitiator(fixSessionManager, messageStoreFactory, sessionSettings, logFactory, messageFactory);
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
		// This method is now delegated to FixSettingsBuilder
		// Keeping for backwards compatibility temporarily
		return settingsBuilder.buildSettings(
				properties.getHeartbeatIntervalSeconds(),
				properties.getReconnectIntervalSeconds(),
				storeDirectory.toAbsolutePath().toString(),
				logDirectory.toAbsolutePath().toString(),
				properties.getSessions());
	}

	@Override
	public void destroy() {
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
}
