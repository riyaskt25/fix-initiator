package com.demo.fix.initiator.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.demo.fix.initiator.message.ExecutionReportXml;
import com.demo.fix.initiator.message.FixMessageFields;
import com.demo.fix.initiator.message.NewOrderSingleXml;
import com.demo.fix.initiator.message.UnknownFixMessageXml;

@Service
public class XmlMessageSerializer {

	public String toXml(FixMessageFields fields, Object payload) {
		XmlEnvelope envelope = XmlEnvelope.from(fields, payload);
		StringBuilder xml = new StringBuilder();
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		xml.append("<fixMessage>\n");
		xml.append("  <header>\n");
		appendTag(xml, "    ", "Beginstring", mapOf("fix", "8"), envelope.beginString());
		appendTag(xml, "    ", "BodyLength", mapOf("fix", "9"), envelope.bodyLength());
		appendTag(xml, "    ", "MsgSeqNum", mapOf("fix", "34"), envelope.msgSeqNum());
		appendTag(xml, "    ", "MsgType", mapOf("fix", "35", "description", envelope.messageTypeDescription()), envelope.messageType());
		appendTag(xml, "    ", "SenderCompID", mapOf("fix", "49"), envelope.senderCompId());
		appendTag(xml, "    ", "SendingTime", mapOf("fix", "52"), envelope.sendingTime());
		appendTag(xml, "    ", "TargetCompID", mapOf("fix", "56"), envelope.targetCompId());
		xml.append("  </header>\n");
		xml.append("  <body>\n");
		for (XmlField field : envelope.bodyFields()) {
			appendTag(xml, "    ", field.elementName(), field.attributes(), field.value());
		}
		xml.append("  </body>\n");
		xml.append("</fixMessage>");
		return xml.toString();
	}

	private void appendTag(StringBuilder xml, String indent, String elementName, Map<String, String> attributes, String value) {
		if (value == null || value.isBlank()) {
			return;
		}
		xml.append(indent).append("<").append(elementName);
		for (Map.Entry<String, String> attribute : attributes.entrySet()) {
			xml.append(" ").append(attribute.getKey()).append("=\"").append(escape(attribute.getValue())).append("\"");
		}
		xml.append(">").append(escape(value)).append("</").append(elementName).append(">\n");
	}

	private static Map<String, String> mapOf(String... entries) {
		Map<String, String> attributes = new LinkedHashMap<>();
		for (int index = 0; index + 1 < entries.length; index += 2) {
			attributes.put(entries[index], entries[index + 1]);
		}
		return attributes;
	}

	private static String escape(String value) {
		return value
				.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&apos;");
	}

	private record XmlField(String elementName, Map<String, String> attributes, String value) {
	}

	private record XmlEnvelope(String beginString, String bodyLength, String msgSeqNum, String messageType,
			String messageTypeDescription, String senderCompId, String sendingTime, String targetCompId,
			XmlField[] bodyFields) {

		static XmlEnvelope from(FixMessageFields fields, Object payload) {
			String description = describe(fields.messageType());
			XmlField[] bodyFields = toBodyFields(payload, fields);
			return new XmlEnvelope(
					defaultString(fields.beginString()),
					defaultString(fields.bodyLength()),
					defaultString(fields.msgSeqNum()),
					defaultString(fields.messageType()),
					description,
					defaultString(fields.senderCompId()),
					defaultString(fields.sendingTime()),
					defaultString(fields.targetCompId()),
					bodyFields);
		}

		private static XmlField[] toBodyFields(Object payload, FixMessageFields fields) {
			if (payload instanceof ExecutionReportXml executionReport) {
				return new XmlField[] {
					new XmlField("AvgPx", mapOf("fix", "6"), null),
					new XmlField("ClordID", mapOf("fix", "11"), executionReport.clOrdId()),
					new XmlField("OrderID", mapOf("fix", "37"), executionReport.orderId()),
					new XmlField("ExecID", mapOf("fix", "17"), executionReport.execId()),
					new XmlField("OrdStatus", mapOf("fix", "39"), executionReport.ordStatus()),
					new XmlField("Text", mapOf("fix", "58"), executionReport.text()),
					new XmlField("TransactTime", mapOf("fix", "60"), executionReport.transactTime())
				};
			}
			if (payload instanceof NewOrderSingleXml newOrderSingle) {
				return new XmlField[] {
					new XmlField("ClordID", mapOf("fix", "11"), newOrderSingle.clOrdId()),
					new XmlField("Symbol", mapOf("fix", "55"), newOrderSingle.symbol()),
					new XmlField("Side", mapOf("fix", "54"), newOrderSingle.side()),
					new XmlField("OrderQty", mapOf("fix", "38"), newOrderSingle.quantity()),
					new XmlField("TransactTime", mapOf("fix", "60"), newOrderSingle.transactTime())
				};
			}
			if (payload instanceof UnknownFixMessageXml unknownFixMessageXml) {
				return new XmlField[] {
					new XmlField("ClordID", mapOf("fix", "11"), unknownFixMessageXml.clOrdId()),
					new XmlField("OrderID", mapOf("fix", "37"), unknownFixMessageXml.orderId()),
					new XmlField("ExecID", mapOf("fix", "17"), unknownFixMessageXml.execId()),
					new XmlField("Symbol", mapOf("fix", "55"), unknownFixMessageXml.symbol()),
					new XmlField("Side", mapOf("fix", "54"), unknownFixMessageXml.side()),
					new XmlField("OrderQty", mapOf("fix", "38"), unknownFixMessageXml.quantity()),
					new XmlField("OrdStatus", mapOf("fix", "39"), unknownFixMessageXml.ordStatus()),
					new XmlField("Text", mapOf("fix", "58"), unknownFixMessageXml.text()),
					new XmlField("TransactTime", mapOf("fix", "60"), unknownFixMessageXml.transactTime())
				};
			}
			return new XmlField[] {
				new XmlField("Text", mapOf("fix", "58"), fields.rawFix())
			};
		}

		private static String describe(String messageType) {
			return switch (messageType) {
				case "8" -> "ExecutionReport";
				case "D" -> "NewOrderSingle";
				case "0" -> "Heartbeat";
				case "A" -> "Logon";
				default -> "Unknown";
			};
		}

		private static String defaultString(String value) {
			return value == null ? "" : value;
		}
	}
}