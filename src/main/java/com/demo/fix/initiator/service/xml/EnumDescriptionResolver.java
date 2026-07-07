package com.demo.fix.initiator.service.xml;

import org.springframework.stereotype.Component;

@Component
public class EnumDescriptionResolver {

	private final FieldNameResolver fieldNameResolver;

	public EnumDescriptionResolver(FieldNameResolver fieldNameResolver) {
		this.fieldNameResolver = fieldNameResolver;
	}

	public String resolve(int tag, String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String description = fieldNameResolver.getDataDictionary().getValueName(tag, value);
		return (description == null || description.isBlank()) ? null : description;
	}
}