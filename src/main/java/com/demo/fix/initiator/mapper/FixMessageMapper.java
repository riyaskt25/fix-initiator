package com.demo.fix.initiator.mapper;

import com.demo.fix.initiator.message.FixMessageFields;

/**
 * Pluggable mapper for a specific FIX message type.
 * Add a new Spring bean implementing this interface to support a new message type.
 */
public interface FixMessageMapper<T> {

	boolean supports(String sessionKey, String messageType);

	T map(FixMessageFields fields);
}