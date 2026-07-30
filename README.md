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

## Java Compatibility

- Project source/target compatibility is set to Java 8.
- Spring Boot version is pinned to 2.7.x for Java 8 support.
- Gradle wrapper is pinned to 6.9.4 so builds can run on Java 8 machines.