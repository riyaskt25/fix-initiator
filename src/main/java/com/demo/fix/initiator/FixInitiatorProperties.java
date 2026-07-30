package com.demo.fix.initiator;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Minimal Spring-managed properties for the FIX initiator.
 * Session configuration lives in initiator.cfg (external or classpath).
 * This class retains only properties that are consumed by Spring beans
 * outside of the QFJ engine itself (e.g. FieldNameResolver).
 */
@ConfigurationProperties(prefix = "fix.initiator")
public class FixInitiatorProperties {

	/**
	 * Classpath resource name of the FIX data dictionary used by FieldNameResolver
	 * for XML tag-name resolution. Must match a file in src/main/resources/.
	 */
	private String dataDictionaryResource = "FIX44.xml";

	public String getDataDictionaryResource() {
		return dataDictionaryResource;
	}

	public void setDataDictionaryResource(String dataDictionaryResource) {
		this.dataDictionaryResource = dataDictionaryResource;
	}
}
