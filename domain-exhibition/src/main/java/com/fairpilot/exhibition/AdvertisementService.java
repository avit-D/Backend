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

    /** 광고 슬롯 목록 조회 */
    @Transactional(readOnly = true)
    public List<AdSlot> findSlots(Long exhibitionId) {
        return adSlotRepository.findAllByExhibitionIdAndStatus(
                exhibitionId, AdSlotStatus.ACTIVE);
    }

    /** 광고 목록 조회 */
    @Transactional(readOnly = true)
    public List<Advertisement> findAds(Long adSlotId) {
        return advertisementRepository.findAllByAdSlotId(adSlotId);
    }

    /** 광고 단건 조회 */
    @Transactional(readOnly = true)
    public Advertisement findById(Long adId) {
        return advertisementRepository.findById(adId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "광고를 찾을 수 없습니다."));
    }

    /** 광고 생성 */
    @Transactional
    public Advertisement create(AdRequest req) {
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
        return advertisementRepository.save(ad);
    }

    /** 노출 카운트 증가 */
    @Transactional
    public void incrementImpressions(Long adId) {
        Advertisement ad = findById(adId);
        ad.incrementImpressions();
        advertisementRepository.save(ad);
    }

    /** 클릭 카운트 증가 */
    @Transactional
    public void incrementClicks(Long adId) {
        Advertisement ad = findById(adId);
        ad.incrementClicks();
        advertisementRepository.save(ad);
    }
}