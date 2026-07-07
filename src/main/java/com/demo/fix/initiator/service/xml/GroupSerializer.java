package com.demo.fix.initiator.service.xml;

import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.stereotype.Component;

import quickfix.Field;
import quickfix.FieldMap;
import quickfix.Group;

@Component
public class GroupSerializer {

	private final FieldNameResolver fieldNameResolver;
	private final EnumDescriptionResolver enumDescriptionResolver;

	public GroupSerializer(FieldNameResolver fieldNameResolver, EnumDescriptionResolver enumDescriptionResolver) {
		this.fieldNameResolver = fieldNameResolver;
		this.enumDescriptionResolver = enumDescriptionResolver;
	}

	public void serializeGroups(XMLStreamWriter writer, FieldMap fieldMap) throws XMLStreamException {
		Iterator<Integer> groupTagIterator = fieldMap.groupKeyIterator();
		while (groupTagIterator.hasNext()) {
			int groupTag = groupTagIterator.next();
			List<Group> groups = fieldMap.getGroups(groupTag);
			if (groups == null || groups.isEmpty()) {
				continue;
			}

			String groupName = fieldNameResolver.resolve(groupTag);
			writer.writeStartElement(groupName);
			writer.writeAttribute("fix", String.valueOf(groupTag));

			for (Group group : groups) {
				writer.writeStartElement("groupEntry");
				serializeFields(writer, group);
				serializeGroups(writer, group);
				writer.writeEndElement();
			}

			writer.writeEndElement();
		}
	}

	public void serializeFields(XMLStreamWriter writer, FieldMap fieldMap) throws XMLStreamException {
		Iterator<Field<?>> fields = fieldMap.iterator();
		while (fields.hasNext()) {
			Field<?> field = fields.next();
			int tag = field.getTag();
			String value = field.getObject() == null ? "" : String.valueOf(field.getObject());
			String elementName = fieldNameResolver.resolve(tag);

			writer.writeStartElement(elementName);
			writer.writeAttribute("fix", String.valueOf(tag));
			String description = enumDescriptionResolver.resolve(tag, value);
			if (description != null) {
				writer.writeAttribute("description", description);
			}
			writer.writeCharacters(value);
			writer.writeEndElement();
		}
	}
}