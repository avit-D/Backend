package com.fairpilot.core.media;

public record S3DeleteEvent(
        String s3Key,
        String bucket
) {}