package com.demo.fix.initiator.service.xml;

import org.springframework.stereotype.Component;

import quickfix.DataDictionary;

/**
 * Resolves a FIX tag number to a human-readable XML element name using the
 * DataDictionary that belongs to the session that produced the message.
 * Stateless — the DataDictionary is supplied per call, not stored.
 */
@Component
public class FieldNameResolver {

	/**
	 * Returns the field name for the given tag from the supplied dictionary,
	 * or "Tag{n}" if the dictionary is null or the tag is not defined.
	 */
	public String resolve(int tag, DataDictionary dictionary) {
		if (dictionary == null) {
			return "Tag" + tag;
		}
		String name = dictionary.getFieldName(tag);
		return (name == null || name.isBlank()) ? "Tag" + tag : sanitizeElementName(name);
	}

	private String sanitizeElementName(String value) {
		return value.replaceAll("[^A-Za-z0-9_]", "");
	}
}