package com.fairpilot.exhibition;

import com.fairpilot.core.common.BusinessException;
import com.fairpilot.core.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdvertisementService {

    private final AdSlotRepository adSlotRepository;
    private final AdvertisementRepository advertisementRepository;

    /** 광고 슬롯 목록 조회 (exhibitionId null = 전체) */
    @Transactional(readOnly = true)
    public List<AdSlotResponse> findSlots(Long exhibitionId) {
        List<AdSlot> slots = exhibitionId == null
                ? adSlotRepository.findAllByStatus(AdSlotStatus.ACTIVE)
                : adSlotRepository.findAllByExhibitionIdAndStatus(exhibitionId, AdSlotStatus.ACTIVE);
        return slots.stream().map(AdSlotResponse::from).toList();
    }

    /** 슬롯 생성 */
    @Transactional
    public AdSlotResponse createSlot(AdSlotRequest req) {
        AdSlot slot = AdSlot.builder()
                .exhibitionId(req.exhibitionId())
                .placement(req.placement())
                .basePrice(req.basePrice())
                .build();
        return AdSlotResponse.from(adSlotRepository.save(slot));
    }

    /** 광고 목록 조회 — ACTIVE 상태만 공개 */
    @Transactional(readOnly = true)
    public List<AdvertisementResponse> findAds(Long adSlotId) {
        return advertisementRepository
                .findAllByAdSlotIdAndStatus(adSlotId, AdStatus.ACTIVE)
                .stream().map(AdvertisementResponse::from).toList();
    }

    /** 광고 생성 */
    @Transactional
    public AdvertisementResponse create(AdRequest req) {
        Advertisement ad = Advertisement.builder()
                .adSlotId(req.adSlotId())
                .advertiserName(req.advertiserName())
                .exhibitorId(req.exhibitorId())
                .title(req.title())
                .imageUrl(req.imageUrl())
                .linkUrl(req.linkUrl())
                .startAt(req.startAt())
                .endAt(req.endAt())
                .price(req.price())
                .build();
        return AdvertisementResponse.from(advertisementRepository.save(ad));
    }

    /** 노출 카운트 증가 — DB 원자 UPDATE */
    @Transactional
    public void incrementImpressions(Long adId) {
        if (!advertisementRepository.existsById(adId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "광고를 찾을 수 없습니다.");
        }
        advertisementRepository.incrementImpressions(adId);
    }

    /** 클릭 카운트 증가 — DB 원자 UPDATE */
    @Transactional
    public void incrementClicks(Long adId) {
        if (!advertisementRepository.existsById(adId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "광고를 찾을 수 없습니다.");
        }
        advertisementRepository.incrementClicks(adId);
    }
}
