package com.demo.fix.initiator.service;

import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Enterprise-grade application keep-alive mechanism.
 * Ensures the JVM stays alive to run the FIX initiator indefinitely,
 * without requiring a web server.
 * 
 * The SocketInitiator's non-blocking threads alone don't keep the JVM alive.
 * This component provides a simple, minimal blocking mechanism.
 */
@Component
public class KeepAliveRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(KeepAliveRunner.class);
	private static final CountDownLatch keepAlive = new CountDownLatch(1);

	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.info("FIX initiator application is ready. Blocking main thread to keep JVM alive.");
		// Block indefinitely - the app will only exit via SIGTERM or exception
		keepAlive.await();
	}

	/**
	 * Graceful shutdown signal (called by shutdown hooks during SIGTERM)
	 */
	public static void shutdown() {
		keepAlive.countDown();
	}
}
