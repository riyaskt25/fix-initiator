package com.demo.fix.initiator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.stereotype.Service;

@Service
public class XmlMessageSerializer {

	private final XmlMapper xmlMapper;

	public XmlMessageSerializer() {
		this.xmlMapper = XmlMapper.builder()
				.defaultUseWrapper(false)
				.build();
	}

	public String toXml(Object payload) throws JsonProcessingException {
		return xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
	}
}