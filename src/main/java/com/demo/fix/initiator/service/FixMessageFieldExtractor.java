package com.demo.fix.initiator.service;

import org.springframework.stereotype.Service;

import com.demo.fix.initiator.message.FixMessageFields;

import quickfix.FieldNotFound;
import quickfix.Message;
import quickfix.SessionID;
import quickfix.field.ClOrdID;
import quickfix.field.BodyLength;
import quickfix.field.ExecID;
import quickfix.field.MsgType;
import quickfix.field.MsgSeqNum;
import quickfix.field.OrderID;
import quickfix.field.OrderQty;
import quickfix.field.OrdStatus;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.Text;
import quickfix.field.TransactTime;
import quickfix.field.SendingTime;

@Service
public class FixMessageFieldExtractor {

	public FixMessageFields extract(Message message, SessionID sessionId) throws FieldNotFound {
		String sessionKey = sanitizeXmlText(sessionId.toString());
		String beginString = sanitizeXmlText(sessionId.getBeginString());
		String bodyLength = getOptionalString(message, BodyLength.FIELD);
		String msgSeqNum = getOptionalString(message, MsgSeqNum.FIELD);
		String senderCompId = sanitizeXmlText(sessionId.getSenderCompID());
		String targetCompId = sanitizeXmlText(sessionId.getTargetCompID());
		String messageType = sanitizeXmlText(message.getHeader().getString(MsgType.FIELD));
		String sendingTime = getOptionalString(message, SendingTime.FIELD);

		return new FixMessageFields(
				sessionKey,
				beginString,
				bodyLength,
				msgSeqNum,
				senderCompId,
				targetCompId,
				messageType,
				sendingTime,
				sanitizeXmlText(message.toString()),
				getOptionalString(message, ClOrdID.FIELD),
				getOptionalString(message, OrderID.FIELD),
				getOptionalString(message, ExecID.FIELD),
				getOptionalString(message, Symbol.FIELD),
				getOptionalString(message, Side.FIELD),
				getOptionalString(message, OrderQty.FIELD),
				getOptionalString(message, OrdStatus.FIELD),
				getOptionalString(message, Text.FIELD),
				getOptionalString(message, TransactTime.FIELD));
	}

	private String getOptionalString(Message message, int field) throws FieldNotFound {
		return message.isSetField(field) ? sanitizeXmlText(message.getString(field)) : null;
	}

	private String sanitizeXmlText(String value) {
		if (value == null) {
			return null;	
		}
		// Replace XML-invalid control chars (including FIX SOH \u0001) with a readable delimiter.
		return value.replaceAll("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]", "|");
	}
}