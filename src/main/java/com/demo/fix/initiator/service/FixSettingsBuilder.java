package com.demo.fix.initiator.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.demo.fix.initiator.FixInitiatorProperties;

/**
 * Builds QFJ session settings configuration.
 * Extracts the settings generation logic from the main service.
 */
@Service
public class FixSettingsBuilder {

	public String buildSettings(
			int heartbeatIntervalSeconds,
			int reconnectIntervalSeconds,
			String storeDirectoryPath,
			String logDirectoryPath,
			String dataDictionaryPath,
			List<FixInitiatorProperties.SessionConfig> sessions) {

		StringBuilder sb = new StringBuilder();
		sb.append("""
				[DEFAULT]
				ConnectionType=initiator
				HeartBtInt=%d
				ReconnectInterval=%d
				FileStorePath=%s
				FileLogPath=%s
				DataDictionary=%s
				StartTime=00:00:00
				EndTime=23:59:59
				UseDataDictionary=Y
				""".formatted(
				heartbeatIntervalSeconds,
				reconnectIntervalSeconds,
				storeDirectoryPath,
				logDirectoryPath,
				dataDictionaryPath));

		for (FixInitiatorProperties.SessionConfig session : sessions) {
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
}
