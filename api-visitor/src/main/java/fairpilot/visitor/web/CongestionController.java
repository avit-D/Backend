package fairpilot.visitor.web;

import com.fairpilot.core.common.ApiResponse;
import com.fairpilot.tracking.congestion.dto.CongestionEvent;
import com.fairpilot.tracking.congestion.service.CongestionService;
import com.fairpilot.tracking.congestion.service.CongestionSseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/congestion")
@RequiredArgsConstructor
public class CongestionController {

    private final CongestionService congestionService;
    private final CongestionSseService sseService;

    /** 실시간 혼잡도 SSE 스트림(연결 시 스냅샷 1회 후 변경분 push). */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam Long exhibitionId) {
        return sseService.subscribe(exhibitionId, congestionService.snapshot(exhibitionId));
    }

    /** 현재 혼잡도 스냅샷. */
    @GetMapping("/live")
    public ApiResponse<List<CongestionEvent>> live(@RequestParam Long exhibitionId) {
        return ApiResponse.ok(congestionService.snapshot(exhibitionId));
    }
}
