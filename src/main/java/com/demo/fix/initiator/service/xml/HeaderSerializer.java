package com.demo.fix.initiator.service.xml;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.stereotype.Component;

import quickfix.DataDictionary;
import quickfix.FieldMap;

@Component
public class HeaderSerializer {

	private final GroupSerializer groupSerializer;

	public HeaderSerializer(GroupSerializer groupSerializer) {
		this.groupSerializer = groupSerializer;
	}

	public void serialize(XMLStreamWriter writer, FieldMap header, DataDictionary dictionary) throws XMLStreamException {
		writer.writeStartElement("header");
		groupSerializer.serializeFields(writer, header, dictionary);
		groupSerializer.serializeGroups(writer, header, dictionary);
		writer.writeEndElement();
	}
}