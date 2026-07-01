package com.demo.fix.initiator.service;

import org.springframework.stereotype.Service;

import com.demo.fix.initiator.message.FixMessageFields;

import quickfix.FieldNotFound;
import quickfix.Message;
import quickfix.SessionID;
import quickfix.field.ClOrdID;
import quickfix.field.ExecID;
import quickfix.field.MsgType;
import quickfix.field.OrderID;
import quickfix.field.OrderQty;
import quickfix.field.OrdStatus;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.Text;
import quickfix.field.TransactTime;

@Service
public class FixMessageFieldExtractor {

	public FixMessageFields extract(Message message, SessionID sessionId) throws FieldNotFound {
		String sessionKey = sessionId.toString();
		String beginString = sessionId.getBeginString();
		String senderCompId = sessionId.getSenderCompID();
		String targetCompId = sessionId.getTargetCompID();
		String messageType = message.getHeader().getString(MsgType.FIELD);

		return new FixMessageFields(
				sessionKey,
				beginString,
				senderCompId,
				targetCompId,
				messageType,
				message.toString(),
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
		return message.isSetField(field) ? message.getString(field) : null;
	}
}