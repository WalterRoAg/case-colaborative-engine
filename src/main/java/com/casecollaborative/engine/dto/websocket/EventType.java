package com.casecollaborative.engine.dto.websocket;

public enum EventType {
    USER_JOINED,
    USER_LEFT,
    CURSOR_MOVED,
    LOCK_REQUESTED,
    LOCK_GRANTED,
    LOCK_DENIED,
    LOCK_RELEASED,
    CLASS_CREATED,
    CLASS_MOVED,
    CLASS_DELETED,
    ATTRIBUTE_MUTATED,
    RELATION_MUTATED,
    ELEMENT_DELETED
}
