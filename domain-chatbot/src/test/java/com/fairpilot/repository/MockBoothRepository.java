package com.fairpilot.repository;

import com.fairpilot.exhibition.Booth;
import com.fairpilot.exhibition.BoothRepository;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * MockBoothRepository — ChatbotRunner 전용 인메모리 구현체
 * ────────────────────────────────────────────────────────────────
 * 실제 JPA 없이 BoothRepository 인터페이스를 구현해 로컬 실행 시 사용.
 * 운영 코드(BoothRepository 의 실제 JPA 구현체)와는 무관하며
 * ChatbotRunner 컴포넌트 스캔 범위 안에서만 @Primary 로 등록됨.
 */
public class MockBoothRepository implements BoothRepository {

    private final Map<Long, Booth> store;

    public MockBoothRepository() {
        List<Booth> samples = List.of(
                build(1001L, 100L, 10L, 1L, "AI 비전 솔루션관",
                        "제조 공정 불량 검출을 위한 엣지 AI 비전 솔루션 전시.",
                        "컴퓨터비전,객체인식,제조AI", 120, 80, 1),

                build(1002L, 100L, 11L, 2L, "스마트 물류 자동화관",
                        "자율주행 로봇(AMR)과 WMS 통합 물류 자동화 플랫폼.",
                        "AMR,WMS,자동화", 200, 80, 1),

                build(1003L, 100L, 12L, 3L, "헬스케어 데이터 플랫폼관",
                        "FHIR 기반 전자의무기록 통합 및 임상 의사결정 지원 AI.",
                        "EHR,의료AI,FHIR", 120, 160, 1),

                build(1004L, 100L, 13L, 4L, "사이버보안 제로트러스트관",
                        "제로트러스트 아키텍처 기반 엔터프라이즈 보안 플랫폼.",
                        "제로트러스트,SIEM,EDR", 200, 160, 1),

                build(1005L, 100L, 14L, 5L, "그린에너지 모니터링관",
                        "태양광·풍력 발전소 실시간 모니터링 및 ESG 리포팅 솔루션.",
                        "ESG,태양광,에너지관리", 120, 80, 2),

                build(1006L, 100L, 15L, 6L, "핀테크 결제 혁신관",
                        "오픈뱅킹 연동 통합 결제 게이트웨이 및 정산 자동화.",
                        "간편결제,오픈뱅킹,PG", 200, 80, 2),

                build(1007L, 100L, 16L, 7L, "메타버스 교육 플랫폼관",
                        "VR 기반 직무 교육 시뮬레이터와 LMS 통합 플랫폼.",
                        "메타버스,VR교육,LMS", 120, 160, 2),

                build(1008L, 100L, 17L, 8L, "스마트팜 IoT관",
                        "IoT 센서 기반 수직농장 자동화 및 작물 생장 예측 AI.",
                        "스마트팜,수직농장,IoT센서", 200, 160, 2)
        );

        this.store = samples.stream()
                .collect(Collectors.toMap(Booth::getId, b -> b));
    }

    /**
     * Booth 생성자가 exhibitionId 등을 직접 받지 않으므로(빌더에 없음)
     * 리플렉션으로 protected 필드를 채워 테스트 데이터 구성.
     * 실제 운영에서는 JPA 가 영속화 시 처리하므로 이 방식은 Mock 전용.
     */
    private Booth build(Long id, Long exhibitionId, Long exhibitorId, Long categoryId,
                        String name, String description, String tags,
                        Integer posX, Integer posY, Integer floor) {
        Booth booth = Booth.builder()
                .exhibitionId(exhibitionId)
                .exhibitorId(exhibitorId)
                .categoryId(categoryId)
                .name(name)
                .description(description)
                .tags(tags)
                .posX(posX)
                .posY(posY)
                .floor(floor)
                .build();
        setId(booth, id);
        return booth;
    }

    private void setId(Booth booth, Long id) {
        try {
            Field idField = Booth.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(booth, id);
        } catch (Exception e) {
            throw new IllegalStateException("Mock ID 설정 실패", e);
        }
    }

    // ── 가드/서비스가 실제로 사용하는 메서드 ─────────────────────────
    @Override
    public boolean existsById(Long id) {
        return store.containsKey(id);
    }

    @Override
    public List<Booth> findAllByExhibitionId(Long exhibitionId) {
        return store.values().stream()
                .filter(b -> Objects.equals(b.getExhibitionId(), exhibitionId))
                .toList();
    }

    @Override
    public List<Booth> findAllById(Iterable<Long> ids) {
        Set<Long> idSet = StreamSupport.stream(ids.spliterator(), false)
                .collect(Collectors.toSet());
        return store.values().stream()
                .filter(b -> idSet.contains(b.getId()))
                .toList();
    }

    // ── 사용하지 않는 JpaRepository 표준 메서드 ───────────────────────
    @Override public <S extends Booth> S save(S entity) { throw new UnsupportedOperationException(); }
    @Override public <S extends Booth> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
    @Override public Optional<Booth> findById(Long id) { return Optional.ofNullable(store.get(id)); }
    @Override public List<Booth> findAll() { return new ArrayList<>(store.values()); }
    @Override public long count() { return store.size(); }
    @Override public void deleteById(Long id) { throw new UnsupportedOperationException(); }
    @Override public void delete(Booth entity) { throw new UnsupportedOperationException(); }
    @Override public void deleteAllById(Iterable<? extends Long> ids) { throw new UnsupportedOperationException(); }
    @Override public void deleteAll(Iterable<? extends Booth> entities) { throw new UnsupportedOperationException(); }
    @Override public void deleteAll() { throw new UnsupportedOperationException(); }
    @Override public List<Booth> findAll(Sort sort) { throw new UnsupportedOperationException(); }
    @Override public Page<Booth> findAll(Pageable pageable) { throw new UnsupportedOperationException(); }
    @Override public void flush() { }
    @Override public <S extends Booth> S saveAndFlush(S entity) { throw new UnsupportedOperationException(); }
    @Override public <S extends Booth> List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
    @Override public void deleteAllInBatch(Iterable<Booth> entities) { throw new UnsupportedOperationException(); }
    @Override public void deleteAllByIdInBatch(Iterable<Long> ids) { throw new UnsupportedOperationException(); }
    @Override public void deleteAllInBatch() { throw new UnsupportedOperationException(); }
    @Override public Booth getOne(Long id) { throw new UnsupportedOperationException(); }
    @Override public Booth getById(Long id) { throw new UnsupportedOperationException(); }
    @Override public Booth getReferenceById(Long id) { throw new UnsupportedOperationException(); }
    @Override public <S extends Booth> Optional<S> findOne(Example<S> example) { throw new UnsupportedOperationException(); }
    @Override public <S extends Booth> List<S> findAll(Example<S> example) { throw new UnsupportedOperationException(); }
    @Override public <S extends Booth> List<S> findAll(Example<S> example, Sort sort) { throw new UnsupportedOperationException(); }
    @Override public <S extends Booth> Page<S> findAll(Example<S> example, Pageable pageable) { throw new UnsupportedOperationException(); }
    @Override public <S extends Booth> long count(Example<S> example) { throw new UnsupportedOperationException(); }
    @Override public <S extends Booth> boolean exists(Example<S> example) { throw new UnsupportedOperationException(); }
    @Override public <S extends Booth, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
}