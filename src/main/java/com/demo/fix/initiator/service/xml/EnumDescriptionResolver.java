package com.demo.fix.initiator.service.xml;

import org.springframework.stereotype.Component;

import quickfix.DataDictionary;

/**
 * Resolves a FIX enum value to its human-readable description using the
 * DataDictionary that belongs to the session that produced the message.
 * Stateless — the DataDictionary is supplied per call, not stored.
 */
@Component
public class EnumDescriptionResolver {

	public String resolve(int tag, String value, DataDictionary dictionary) {
		if (value == null || value.isBlank() || dictionary == null) {
			return null;
		}
		String description = dictionary.getValueName(tag, value);
		return (description == null || description.isBlank()) ? null : description;
	}
}