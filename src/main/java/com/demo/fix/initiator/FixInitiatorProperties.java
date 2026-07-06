package com.demo.fix.initiator;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fix.initiator")
public class FixInitiatorProperties {

	private int heartbeatIntervalSeconds = 30;
	private int reconnectIntervalSeconds = 5;
	private boolean resetStoreOnStart = false;
	private String dataDictionaryResource = "FIX44-Bloomberg.xml";
	private List<SessionConfig> sessions = new ArrayList<>();

	public int getHeartbeatIntervalSeconds() {
		return heartbeatIntervalSeconds;
	}

	public void setHeartbeatIntervalSeconds(int heartbeatIntervalSeconds) {
		this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
	}

	public int getReconnectIntervalSeconds() {
		return reconnectIntervalSeconds;
	}

	public void setReconnectIntervalSeconds(int reconnectIntervalSeconds) {
		this.reconnectIntervalSeconds = reconnectIntervalSeconds;
	}

	public boolean isResetStoreOnStart() {
		return resetStoreOnStart;
	}

	public void setResetStoreOnStart(boolean resetStoreOnStart) {
		this.resetStoreOnStart = resetStoreOnStart;
	}

	public String getDataDictionaryResource() {
		return dataDictionaryResource;
	}

	public void setDataDictionaryResource(String dataDictionaryResource) {
		this.dataDictionaryResource = dataDictionaryResource;
	}

	public List<SessionConfig> getSessions() {
		return sessions;
	}

	public void setSessions(List<SessionConfig> sessions) {
		this.sessions = sessions;
	}

	public static class SessionConfig {

		private String host = "127.0.0.1";
		private int port = 9878;
		private String beginString = "FIX.4.4";
		private String senderCompId = "INITIATOR";
		private String targetCompId = "ACCEPTOR";

		public String getHost() {
			return host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
		}

		public String getBeginString() {
			return beginString;
		}

		public void setBeginString(String beginString) {
			this.beginString = beginString;
		}

		public String getSenderCompId() {
			return senderCompId;
		}

		public void setSenderCompId(String senderCompId) {
			this.senderCompId = senderCompId;
		}

		public String getTargetCompId() {
			return targetCompId;
		}

		public void setTargetCompId(String targetCompId) {
			this.targetCompId = targetCompId;
		}
	}
}
