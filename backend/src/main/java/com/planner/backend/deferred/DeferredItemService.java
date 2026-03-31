package com.planner.backend.deferred;

import com.planner.backend.auth.AppUser;
import com.planner.backend.deferred.dto.DeferredItemCreateRequest;
import com.planner.backend.deferred.dto.DeferredItemResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional
public class DeferredItemService {

    private final DeferredItemRepository repository;

    public DeferredItemService(DeferredItemRepository repository) {
        this.repository = repository;
    }

    public DeferredItemResponse create(AppUser user, DeferredItemCreateRequest request) {
        DeferredItem item = new DeferredItem(user, request.rawText());
        return DeferredItemResponse.from(repository.save(item));
    }

    @Transactional(readOnly = true)
    public List<DeferredItemResponse> listPending(AppUser user) {
        LocalDate today = LocalDate.now(ZoneId.of(user.getTimezone()));
        return repository.findPendingForUser(user, today).stream()
                .map(DeferredItemResponse::from)
                .toList();
    }
}
