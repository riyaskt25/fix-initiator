package com.demo.fix.initiator.service.xml;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.stereotype.Component;

import quickfix.FieldMap;

@Component
public class TrailerSerializer {

	private final GroupSerializer groupSerializer;

	public TrailerSerializer(GroupSerializer groupSerializer) {
		this.groupSerializer = groupSerializer;
	}

	public void serialize(XMLStreamWriter writer, FieldMap trailer) throws XMLStreamException {
		writer.writeStartElement("trailer");
		groupSerializer.serializeFields(writer, trailer);
		groupSerializer.serializeGroups(writer, trailer);
		writer.writeEndElement();
	}
}