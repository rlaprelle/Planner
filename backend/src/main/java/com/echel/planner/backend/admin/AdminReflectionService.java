package com.echel.planner.backend.admin;

import com.echel.planner.backend.admin.dto.AdminReflectionRequest;
import com.echel.planner.backend.admin.dto.AdminReflectionResponse;
import com.echel.planner.backend.common.EntityNotFoundException;
import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.auth.AppUserRepository;
import com.echel.planner.backend.reflection.DailyReflection;
import com.echel.planner.backend.reflection.DailyReflectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AdminReflectionService {

    private final DailyReflectionRepository reflectionRepository;
    private final AppUserRepository userRepository;

    public AdminReflectionService(DailyReflectionRepository reflectionRepository,
                                  AppUserRepository userRepository) {
        this.reflectionRepository = reflectionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminReflectionResponse> listAll() {
        return reflectionRepository.findAll().stream()
                .map(AdminReflectionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminReflectionResponse get(UUID id) {
        return AdminReflectionResponse.from(findReflection(id));
    }

    public AdminReflectionResponse create(AdminReflectionRequest request) {
        AppUser user = userRepository.findById(request.userId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.userId()));
        DailyReflection reflection = new DailyReflection(user, request.reflectionDate());
        reflection.setEnergyRating(request.energyRating());
        reflection.setMoodRating(request.moodRating());
        reflection.setReflectionNotes(request.reflectionNotes());
        if (request.isFinalized() != null) {
            reflection.setFinalized(request.isFinalized());
        }
        return AdminReflectionResponse.from(reflectionRepository.save(reflection));
    }

    public AdminReflectionResponse update(UUID id, AdminReflectionRequest request) {
        DailyReflection reflection = findReflection(id);
        reflection.setEnergyRating(request.energyRating());
        reflection.setMoodRating(request.moodRating());
        reflection.setReflectionNotes(request.reflectionNotes());
        if (request.isFinalized() != null) {
            reflection.setFinalized(request.isFinalized());
        }
        return AdminReflectionResponse.from(reflection);
    }

    public void delete(UUID id) {
        findReflection(id);
        reflectionRepository.deleteById(id);
    }

    private DailyReflection findReflection(UUID id) {
        return reflectionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reflection not found: " + id));
    }
}
