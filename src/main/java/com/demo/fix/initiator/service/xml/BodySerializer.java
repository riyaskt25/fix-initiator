package com.demo.fix.initiator.service.xml;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.stereotype.Component;

import quickfix.DataDictionary;
import quickfix.Message;

@Component
public class BodySerializer {

	private final GroupSerializer groupSerializer;

	public BodySerializer(GroupSerializer groupSerializer) {
		this.groupSerializer = groupSerializer;
	}

	public void serialize(XMLStreamWriter writer, Message message, DataDictionary dictionary) throws XMLStreamException {
		writer.writeStartElement("body");
		groupSerializer.serializeFields(writer, message, dictionary);
		groupSerializer.serializeGroups(writer, message, dictionary);
		writer.writeEndElement();
	}
}