package com.fairpilot.reservation.dto;

public record QueueStatusResponse(long position, boolean allowed, String status) {
    public static QueueStatusResponse of(long position, boolean allowed) {
        String s = position < 0 ? "NOT_IN_QUEUE" : (allowed ? "ALLOWED" : "WAITING");
        return new QueueStatusResponse(position, allowed, s);
    }
}
