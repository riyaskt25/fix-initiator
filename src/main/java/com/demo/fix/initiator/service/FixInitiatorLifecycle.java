package com.demo.fix.initiator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import com.demo.fix.initiator.QuickFixInitiatorService;

/**
 * Manages the FIX initiator lifecycle using Spring's SmartLifecycle.
 * Ensures graceful startup and shutdown with proper ordering.
 * 
 * Phase: Integer.MAX_VALUE means start LAST and stop FIRST.
 * This ensures other beans are ready before FIX connections are opened.
 */
@Component
public class FixInitiatorLifecycle implements SmartLifecycle {

	private static final Logger log = LoggerFactory.getLogger(FixInitiatorLifecycle.class);

	private volatile boolean running = false;
	private final QuickFixInitiatorService fixInitiatorService;

	public FixInitiatorLifecycle(QuickFixInitiatorService fixInitiatorService) {
		this.fixInitiatorService = fixInitiatorService;
	}

	@Override
	public void start() {
		if (running) {
			log.warn("FIX initiator already running");
			return;
		}
		try {
			log.info("Starting FIX initiator lifecycle...");
			fixInitiatorService.start();
			running = true;
			log.info("FIX initiator lifecycle started successfully");
		} catch (IllegalStateException e) {
			// No sessions configured - log warning but allow app to start
			// Sessions can be configured via environment, REST endpoints, or later dynamically
			log.warn("FIX initiator not starting: {}", e.getMessage());
			running = false;
		} catch (Exception e) {
			log.error("Failed to start FIX initiator", e);
			running = false;
			throw new RuntimeException("FIX initiator startup failed", e);
		}
	}

	@Override
	public void stop() {
		if (!running) {
			log.warn("FIX initiator is not running");
			return;
		}
		try {
			log.info("Stopping FIX initiator lifecycle...");
			fixInitiatorService.shutdown();
			running = false;
			log.info("FIX initiator lifecycle stopped gracefully");
		} catch (Exception e) {
			log.error("Error during FIX initiator shutdown", e);
		}
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public int getPhase() {
		// Integer.MAX_VALUE: start LAST (after all other beans), stop FIRST (before other beans)
		// This ensures database, caches, etc. are ready before FIX connections open
		return Integer.MAX_VALUE;
	}

	@Override
	public boolean isAutoStartup() {
		// Auto-start when Spring context starts
		return true;
	}

	@Override
	public void stop(Runnable callback) {
		stop();
		callback.run();
	}
}
