package com.demo.fix.initiator.service.xml;

import java.io.IOException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.demo.fix.initiator.FixInitiatorProperties;

import quickfix.ConfigError;
import quickfix.DataDictionary;

@Component
public class FieldNameResolver {

	private final DataDictionary dataDictionary;

	public FieldNameResolver(FixInitiatorProperties properties) {
		try {
			ClassPathResource resource = new ClassPathResource(properties.getDataDictionaryResource());
			this.dataDictionary = new DataDictionary(resource.getInputStream());
		} catch (IOException | ConfigError e) {
			throw new IllegalStateException("Unable to load FIX dictionary for field name resolution", e);
		}
	}

	public String resolve(int tag) {
		String name = dataDictionary.getFieldName(tag);
		if (name == null || name.isBlank()) {
			return "Tag" + tag;
		}
		return sanitizeElementName(name);
	}

	public DataDictionary getDataDictionary() {
		return dataDictionary;
	}

	private String sanitizeElementName(String value) {
		return value.replaceAll("[^A-Za-z0-9_]", "");
	}
}