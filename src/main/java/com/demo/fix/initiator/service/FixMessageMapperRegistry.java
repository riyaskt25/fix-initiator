package com.demo.fix.initiator.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.demo.fix.initiator.mapper.FixMessageMapper;

@Service
public class FixMessageMapperRegistry {

	private final List<FixMessageMapper<?>> mappers;

	public FixMessageMapperRegistry(List<FixMessageMapper<?>> mappers) {
		this.mappers = mappers;
	}

	public Optional<FixMessageMapper<?>> resolve(String sessionKey, String messageType) {
		return mappers.stream()
				.filter(mapper -> mapper.supports(sessionKey, messageType))
				.findFirst();
	}
}