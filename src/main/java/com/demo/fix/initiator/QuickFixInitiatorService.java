package com.demo.fix.initiator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import com.demo.fix.initiator.service.FixSessionManager;
import com.demo.fix.initiator.service.SessionDictionaryRegistry;

import quickfix.ConfigError;
import quickfix.DataDictionary;
import quickfix.DefaultMessageFactory;
import quickfix.FileLogFactory;
import quickfix.FileStoreFactory;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.SocketInitiator;

/**
 * Orchestrates FIX initiator startup and shutdown.
 * Lifecycle is managed by FixInitiatorLifecycle (SmartLifecycle).
 *
 * Settings file resolution (priority order):
 *   1. initiator.cfg in the JVM working directory (next to the running jar)
 *   2. initiator.cfg on the classpath (src/main/resources/initiator.cfg)
 *
 * DataDictionary files referenced in the config are automatically extracted
 * from the classpath to the path specified in each [SESSION] block,
 * so placing dictionary XML files in src/main/resources/ is sufficient.
 */
@Component
public class QuickFixInitiatorService implements DisposableBean {

	private static final Logger log = LoggerFactory.getLogger(QuickFixInitiatorService.class);
	private static final String SETTINGS_FILENAME = "initiator.cfg";
	private static final String DATA_DICTIONARY_KEY = "DataDictionary";

	private final FixSessionManager fixSessionManager;
	private final SessionDictionaryRegistry dictionaryRegistry;
	private volatile SocketInitiator initiator;

	public QuickFixInitiatorService(FixSessionManager fixSessionManager, SessionDictionaryRegistry dictionaryRegistry) {
		this.fixSessionManager = fixSessionManager;
		this.dictionaryRegistry = dictionaryRegistry;
	}

	/**
	 * Starts the FIX initiator.
	 * Called by FixInitiatorLifecycle during Spring context startup.
	 */
	public void start() throws Exception {
		synchronized (this) {
			if (initiator != null) {
				log.warn("FIX initiator already started");
				return;
			}
			startInternal();
		}
	}

	/**
	 * Shuts down the FIX initiator gracefully.
	 * Called by FixInitiatorLifecycle during Spring context shutdown or by DisposableBean.
	 */
	public void shutdown() throws Exception {
		log.info("Initiating FIX initiator shutdown...");
		SocketInitiator currentInitiator = initiator;
		if (currentInitiator != null) {
			currentInitiator.stop();
			initiator = null;
			log.info("FIX initiator stopped successfully");
		}
	}

	private synchronized void startInternal() throws Exception {
		if (initiator != null) {
			return;
		}

		SessionSettings sessionSettings = loadSessionSettings();
		extractDictionaries(sessionSettings);
		populateDictionaryRegistry(sessionSettings);

		MessageStoreFactory messageStoreFactory = new FileStoreFactory(sessionSettings);
		FileLogFactory logFactory = new FileLogFactory(sessionSettings);
		MessageFactory messageFactory = new DefaultMessageFactory();

		initiator = new SocketInitiator(fixSessionManager, messageStoreFactory, sessionSettings, logFactory, messageFactory);
		initiator.start();
		int sessionCount = 0;
		Iterator<SessionID> counter = sessionSettings.sectionIterator();
		while (counter.hasNext()) { counter.next(); sessionCount++; }
		log.info("FIX initiator started with {} session(s)", sessionCount);
	}

	/**
	 * Loads SessionSettings using a two-step priority:
	 * 1. initiator.cfg in the JVM working directory (external override)
	 * 2. initiator.cfg from the classpath (bundled default)
	 */
	private SessionSettings loadSessionSettings() throws ConfigError, IOException {
		Path externalCfg = Path.of(SETTINGS_FILENAME);
		if (Files.exists(externalCfg)) {
			log.info("Loading FIX settings from external file: {}", externalCfg.toAbsolutePath());
			return new SessionSettings(externalCfg.toString());
		}
		log.info("No external {} found in working directory — loading from classpath", SETTINGS_FILENAME);
		try (InputStream is = getClass().getClassLoader().getResourceAsStream(SETTINGS_FILENAME)) {
			if (is == null) {
				throw new IllegalStateException(
						"No FIX settings file found. Place initiator.cfg next to the jar "
								+ "or add it to src/main/resources/");
			}
			return new SessionSettings(is);
		}
	}

	/**
	 * Loads a DataDictionary for each session from the path declared in the cfg
	 * and registers it in the SessionDictionaryRegistry so that serializers can
	 * perform session-specific tag-name and enum-description resolution.
	 * Called after extractDictionaries() so the files are guaranteed to be on disk.
	 */
	private void populateDictionaryRegistry(SessionSettings sessionSettings) {
		Iterator<SessionID> it = sessionSettings.sectionIterator();
		while (it.hasNext()) {
			SessionID sid = it.next();
			try {
				String dictPath = sessionSettings.getString(sid, DATA_DICTIONARY_KEY);
				if (dictPath != null && !dictPath.isBlank()) {
					dictionaryRegistry.register(sid, new DataDictionary(dictPath));
					log.info("Registered DataDictionary for session {}: {}", sid, dictPath);
				}
			} catch (ConfigError e) {
				log.warn("DataDictionary not configured for session {} — tag names will fall back to Tag<n>", sid);
			}
		}
	}

	/**
	 * For every DataDictionary path declared in the loaded session settings,
	 * extracts the corresponding XML file from the classpath if it does not
	 * already exist on disk. This allows dictionary files to be bundled inside
	 * the jar (src/main/resources/) and still be referenced by the cfg file
	 * using a plain file path.
	 */
	private void extractDictionaries(SessionSettings sessionSettings) throws IOException {
		Set<String> dictionaryPaths = new LinkedHashSet<>();
		Iterator<SessionID> sessions = sessionSettings.sectionIterator();
		while (sessions.hasNext()) {
			SessionID sessionId = sessions.next();
			try {
				String path = sessionSettings.getString(sessionId, DATA_DICTIONARY_KEY);
				if (path != null && !path.isBlank()) {
					dictionaryPaths.add(path);
				}
			} catch (ConfigError e) {
				// DataDictionary not set for this session — skip
			}
		}

		for (String dictPath : dictionaryPaths) {
			Path dictFile = Path.of(dictPath);
			if (Files.exists(dictFile)) {
				log.debug("Dictionary already present at {}", dictFile.toAbsolutePath());
				continue;
			}
			String resourceName = dictFile.getFileName().toString();
			try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName)) {
				if (is == null) {
					throw new IllegalStateException(
							"Dictionary '" + resourceName + "' not found on classpath. "
									+ "Add it to src/main/resources/ or place the file at: "
									+ dictFile.toAbsolutePath());
				}
				Files.createDirectories(dictFile.getParent());
				Files.copy(is, dictFile, StandardCopyOption.REPLACE_EXISTING);
				log.info("Extracted dictionary {} from classpath to {}", resourceName, dictFile.toAbsolutePath());
			}
		}
	}

	@Override
	public void destroy() throws Exception {
		// Backup shutdown mechanism (DisposableBean interface)
		// Primary shutdown is handled by FixInitiatorLifecycle
		shutdown();
	}
}
