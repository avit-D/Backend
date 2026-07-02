package com.fairpilot.core.media;

import com.fairpilot.core.common.BusinessException;
import com.fairpilot.core.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaAssetService {

    private final MediaAssetRepository mediaAssetRepository;
    private final S3Client s3Client;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.region}")
    private String region;

    /**
     * 파일 업로드
     *
     * [순서]
     * 1. 파일 유효성 검증 (magic bytes + 크기)
     * 2. S3 key 생성
     * 3. DB 먼저 저장 (트랜잭션 커밋)
     * 4. 트랜잭션 커밋 후 S3 업로드 (@TransactionalEventListener)
     *    → DB 롤백 시 S3 업로드 자체가 발생하지 않음
     *    → S3 실패 시 DB 레코드는 남지만 url로 접근 불가 상태
     *      (운영 시 status 컬럼 추가 or 모니터링으로 감지)
     */
    @Transactional
    public MediaAssetResponse upload(MultipartFile file,
                                     MediaUploadRequest req,
                                     Long uploadedBy) {
        // 1. 파일 유효성 검증 (magic bytes)
        validateFile(file);

        // 2. S3 key 생성
        String ext = getExtension(file.getOriginalFilename());
        String s3Key = req.ownerType().name().toLowerCase() + "/"
                + req.ownerId() + "/"
                + UUID.randomUUID().toString().replace("-", "")
                + (ext.isEmpty() ? "" : "." + ext);

        MediaType mediaType = isVideo(file) ? MediaType.VIDEO : MediaType.IMAGE;

        // 3. DB 먼저 저장
        MediaAsset asset = MediaAsset.builder()
                .ownerType(req.ownerType())
                .ownerId(req.ownerId())
                .mediaType(mediaType)
                .s3Key(s3Key)
                .s3Bucket(bucket)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .displayOrder(req.displayOrder())
                .isThumbnail(req.isThumbnail())
                .uploadedBy(uploadedBy)
                .build();

        mediaAssetRepository.save(asset);

        // 4. 트랜잭션 커밋 후 S3 업로드 이벤트 발행
        try {
            byte[] bytes = file.getBytes();
            eventPublisher.publishEvent(
                    new S3UploadEvent(s3Key, bytes, file.getContentType(), file.getSize()));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "파일 읽기 실패");
        }

        log.info("미디어 DB 저장 완료: id={}, s3Key={}", asset.getId(), s3Key);
        return MediaAssetResponse.of(asset, buildUrl(s3Key));
    }

    /**
     * 트랜잭션 커밋 후 S3 업로드
     * DB 롤백 시 이 메서드는 실행되지 않음
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleS3Upload(S3UploadEvent event) {
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(event.s3Key())
                    .contentType(event.contentType())
                    .contentLength(event.fileSize())
                    .build();

            s3Client.putObject(putRequest,
                    RequestBody.fromBytes(event.bytes()));

            log.info("S3 업로드 완료: bucket={}, key={}", bucket, event.s3Key());

        } catch (Exception e) {
            // S3 실패 로그 — 운영 시 알람 연동 필요
            log.error("S3 업로드 실패: key={}, error={}", event.s3Key(), e.getMessage());
        }
    }

    /**
     * owner별 미디어 목록 조회
     */
    @Transactional(readOnly = true)
    public List<MediaAssetResponse> findByOwner(OwnerType ownerType, Long ownerId) {
        return mediaAssetRepository
                .findByOwnerTypeAndOwnerIdOrderByDisplayOrderAsc(ownerType, ownerId)
                .stream()
                .map(a -> MediaAssetResponse.of(a, buildUrl(a.getS3Key())))
                .collect(Collectors.toList());
    }

    /**
     * 미디어 삭제 (Soft Delete + S3 삭제)
     */
    @Transactional
    public void delete(Long mediaId) {
        MediaAsset asset = mediaAssetRepository.findById(mediaId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "미디어를 찾을 수 없습니다."));

        String s3Key = asset.getS3Key();
        String s3Bucket = asset.getS3Bucket();

        // DB Soft Delete 먼저
        mediaAssetRepository.delete(asset);

        // 커밋 후 S3 삭제
        eventPublisher.publishEvent(new S3DeleteEvent(s3Key, s3Bucket));
        log.info("미디어 삭제 완료: id={}", mediaId);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleS3Delete(S3DeleteEvent event) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(event.bucket())
                    .key(event.s3Key())
                    .build());
            log.info("S3 삭제 완료: key={}", event.s3Key());
        } catch (Exception e) {
            log.error("S3 삭제 실패: key={}, error={}", event.s3Key(), e.getMessage());
        }
    }

    // ── private 유틸 ────────────────────────────────────────

    /**
     * magic bytes(파일 시그니처) 검증
     * Content-Type 헤더는 클라이언트 조작 가능 → 실제 파일 바이트로 검증
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "파일이 없습니다.");
        }

        try {
            byte[] header = file.getBytes();

            if (!isValidMagicBytes(header)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT,
                        "이미지 또는 영상 파일만 업로드 가능합니다. (jpg/png/gif/webp/mp4/mov)");
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "파일 읽기 실패");
        }

        // 파일 크기 검증
        String contentType = file.getContentType();
        long maxSize = (contentType != null && contentType.startsWith("video/"))
                ? 500L * 1024 * 1024
                : 10L * 1024 * 1024;

        if (file.getSize() > maxSize) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "파일 크기 초과: " +
                            ((contentType != null && contentType.startsWith("video/"))
                                    ? "500MB" : "10MB"));
        }
    }

    /**
     * magic bytes 검증
     * JPEG: FF D8 FF
     * PNG:  89 50 4E 47
     * GIF:  47 49 46 38
     * WEBP: 52 49 46 46 ... 57 45 42 50
     * MP4:  66 74 79 70 (offset 4)
     * MOV:  66 74 79 70 (offset 4)
     */
    private boolean isValidMagicBytes(byte[] bytes) {
        if (bytes.length < 12) return false;

        // JPEG
        if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF)
            return true;

        // PNG
        if (bytes[0] == (byte) 0x89 && bytes[1] == 0x50 &&
                bytes[2] == 0x4E && bytes[3] == 0x47)
            return true;

        // GIF
        if (bytes[0] == 0x47 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x38)
            return true;

        // WEBP (RIFF....WEBP)
        if (bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46 &&
                bytes[8] == 0x57 && bytes[9] == 0x45 && bytes[10] == 0x42 && bytes[11] == 0x50)
            return true;

        // MP4 / MOV (ftyp at offset 4)
        if (bytes.length >= 8 &&
                bytes[4] == 0x66 && bytes[5] == 0x74 && bytes[6] == 0x79 && bytes[7] == 0x70)
            return true;

        return false;
    }

    private boolean isVideo(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("video/");
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private String buildUrl(String s3Key) {
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + s3Key;
    }
}