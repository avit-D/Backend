package com.fairpilot.education;

import com.fairpilot.core.common.BusinessException;
import com.fairpilot.core.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EducationService {

    private final EducationGuideRepository guideRepository;
    private final EducationCompletionRepository completionRepository;

    /** 가이드 목록 조회 (정답 제외) */
    @Transactional(readOnly = true)
    public List<EducationGuide> findGuides(TargetRole role) {
        return guideRepository.findAllByTargetRoleAndStatus(role, GuideStatus.ACTIVE);
    }

    /** 가이드 단건 조회 */
    @Transactional(readOnly = true)
    public EducationGuide findGuide(Long guideId) {
        return guideRepository.findById(guideId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "가이드를 찾을 수 없습니다."));
    }

    /** 영상 시청 완료 처리 */
    @Transactional
    public void markVideoCompleted(Long guideId, Long userId) {
        EducationCompletion completion = completionRepository
                .findByGuideIdAndUserId(guideId, userId)
                .orElseGet(() -> completionRepository.save(
                        EducationCompletion.builder()
                                .guideId(guideId)
                                .userId(userId)
                                .videoCompleted(false)
                                .build()));
        completion.markVideoCompleted();
        completionRepository.save(completion);
    }

    /** 텍스트 가이드 확인 완료 처리 */
    @Transactional
    public void markPassed(Long guideId, Long userId) {
        EducationCompletion completion = completionRepository
                .findByGuideIdAndUserId(guideId, userId)
                .orElseGet(() -> EducationCompletion.builder()
                        .guideId(guideId)
                        .userId(userId)
                        .videoCompleted(false)
                        .build());
        completion.markPassed();
        completionRepository.save(completion);
    }

    /**
     * LMS 자격 판정
     * 필수 가이드를 모두 이수했는지 확인
     * enforce_staff_qualification 하드 게이트용
     */
    @Transactional(readOnly = true)
    public boolean isQualified(Long userId, Long exhibitionId, TargetRole role) {
        long required = guideRepository.countRequired(role, exhibitionId);
        long completed = completionRepository.countCompleted(userId, exhibitionId);
        return required > 0 && completed >= required;
    }
}