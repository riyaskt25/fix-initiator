package com.demo.fix.initiator.service.xml;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.stereotype.Component;

import quickfix.DataDictionary;
import quickfix.FieldMap;

@Component
public class TrailerSerializer {

	private final GroupSerializer groupSerializer;

	public TrailerSerializer(GroupSerializer groupSerializer) {
		this.groupSerializer = groupSerializer;
	}

	public void serialize(XMLStreamWriter writer, FieldMap trailer, DataDictionary dictionary) throws XMLStreamException {
		writer.writeStartElement("trailer");
		groupSerializer.serializeFields(writer, trailer, dictionary);
		groupSerializer.serializeGroups(writer, trailer, dictionary);
		writer.writeEndElement();
	}
}