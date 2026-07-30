package com.demo.fix.initiator.service.xml;

import java.io.StringWriter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.stereotype.Service;

import com.demo.fix.initiator.exception.FixIntegrationException;
import com.demo.fix.initiator.service.SessionDictionaryRegistry;

import quickfix.DataDictionary;
import quickfix.FieldNotFound;
import quickfix.Message;
import quickfix.SessionID;

@Service
public class GenericFixXmlSerializer {

	private final HeaderSerializer headerSerializer;
	private final BodySerializer bodySerializer;
	private final TrailerSerializer trailerSerializer;
	private final XmlStreamWriterAdapter xmlStreamWriterAdapter;
	private final SessionDictionaryRegistry dictionaryRegistry;

	public GenericFixXmlSerializer(
			HeaderSerializer headerSerializer,
			BodySerializer bodySerializer,
			TrailerSerializer trailerSerializer,
			XmlStreamWriterAdapter xmlStreamWriterAdapter,
			SessionDictionaryRegistry dictionaryRegistry) {
		this.headerSerializer = headerSerializer;
		this.bodySerializer = bodySerializer;
		this.trailerSerializer = trailerSerializer;
		this.xmlStreamWriterAdapter = xmlStreamWriterAdapter;
		this.dictionaryRegistry = dictionaryRegistry;
	}

	/**
	 * Serializes a FIX message to XML using the DataDictionary registered for the
	 * given session. Tag names and enum descriptions are resolved from that
	 * session-specific dictionary, so sessions using different FIX variants
	 * (e.g. standard FIX44 vs Bloomberg FIX44) are each serialized correctly.
	 */
	public String serialize(Message message, SessionID sessionId) {
		String messageType = readHeaderField(message, 35);
		String sessionKey = sessionId.toString();
		DataDictionary dictionary = dictionaryRegistry.get(sessionId);

		try {
			StringWriter output = new StringWriter();
			XMLStreamWriter writer = xmlStreamWriterAdapter.createWriter(output);

			writer.writeStartDocument("UTF-8", "1.0");
			writer.writeCharacters("\n");
			writer.writeStartElement("fixMessage");
			writer.writeCharacters("\n");

			headerSerializer.serialize(writer, message.getHeader(), dictionary);
			writer.writeCharacters("\n");
			bodySerializer.serialize(writer, message, dictionary);
			writer.writeCharacters("\n");
			trailerSerializer.serialize(writer, message.getTrailer(), dictionary);
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

	private String readHeaderField(Message message, int tag) {
		if (!message.getHeader().isSetField(tag)) {
			return "UNKNOWN";
		}
		try {
			return message.getHeader().getString(tag);
		} catch (FieldNotFound e) {
			return "UNKNOWN";
		}
	}
}