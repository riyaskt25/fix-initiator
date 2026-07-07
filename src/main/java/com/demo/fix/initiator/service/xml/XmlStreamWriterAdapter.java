package com.demo.fix.initiator.service.xml;

import java.io.StringWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.stereotype.Component;

@Component
public class XmlStreamWriterAdapter {

	private final XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();

	public XMLStreamWriter createWriter(StringWriter output) throws XMLStreamException {
		return outputFactory.createXMLStreamWriter(output);
	}
}