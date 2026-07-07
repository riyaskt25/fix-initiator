package com.demo.fix.initiator.service.xml;

import java.io.StringWriter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.stereotype.Service;

import com.demo.fix.initiator.exception.FixIntegrationException;

import quickfix.FieldNotFound;
import quickfix.Message;

@Service
public class GenericFixXmlSerializer {

	private final HeaderSerializer headerSerializer;
	private final BodySerializer bodySerializer;
	private final TrailerSerializer trailerSerializer;
	private final XmlStreamWriterAdapter xmlStreamWriterAdapter;

	public GenericFixXmlSerializer(
			HeaderSerializer headerSerializer,
			BodySerializer bodySerializer,
			TrailerSerializer trailerSerializer,
			XmlStreamWriterAdapter xmlStreamWriterAdapter) {
		this.headerSerializer = headerSerializer;
		this.bodySerializer = bodySerializer;
		this.trailerSerializer = trailerSerializer;
		this.xmlStreamWriterAdapter = xmlStreamWriterAdapter;
	}

	public String serialize(Message message) {
		String messageType = readHeaderField(message, 35);
		String sessionKey = readHeaderField(message, 49) + "->" + readHeaderField(message, 56);

		try {
			StringWriter output = new StringWriter();
			XMLStreamWriter writer = xmlStreamWriterAdapter.createWriter(output);

			writer.writeStartDocument("UTF-8", "1.0");
			writer.writeCharacters("\n");
			writer.writeStartElement("fixMessage");
			writer.writeCharacters("\n");

			headerSerializer.serialize(writer, message.getHeader());
			writer.writeCharacters("\n");
			bodySerializer.serialize(writer, message);
			writer.writeCharacters("\n");
			trailerSerializer.serialize(writer, message.getTrailer());
			writer.writeCharacters("\n");

			writer.writeEndElement();
			writer.writeEndDocument();
			writer.flush();
			writer.close();

			return output.toString();
		} catch (XMLStreamException e) {
			throw new FixIntegrationException(sessionKey, messageType, "Failed to serialize FIX message to XML");
		}
	}

	private String safeValue(String value) {
		return value == null ? "UNKNOWN" : value;
	}

	private String readHeaderField(Message message, int tag) {
		if (!message.getHeader().isSetField(tag)) {
			return "UNKNOWN";
		}
		try {
			return safeValue(message.getHeader().getString(tag));
		} catch (FieldNotFound e) {
			return "UNKNOWN";
		}
	}
}