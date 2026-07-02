package com.fairpilot.core.media;

public record S3UploadEvent(
        String s3Key,
        byte[] bytes,
        String contentType,
        long fileSize
) {}