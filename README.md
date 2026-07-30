# fix-initiator

Spring Boot FIX initiator built on QuickFIX/J.

## Current Message Flow

1. `FixSessionManager.fromApp()` receives inbound FIX messages.
2. `FixMessagePublicationService` forwards to `DummyXmlBrokerPublisher`.
3. `DummyXmlBrokerPublisher` serializes the message with `GenericFixXmlSerializer`.
4. XML serialization is implemented with `XMLStreamWriter` and dictionary-aware serializers:
	- `HeaderSerializer`
	- `BodySerializer`
	- `GroupSerializer`
	- `TrailerSerializer`

## Notes

- XML output is generated with JDK StAX (`XMLStreamWriter`) and not Jackson XML.
- The legacy MapStruct/Jackson mapper DTO pipeline has been removed as unused.