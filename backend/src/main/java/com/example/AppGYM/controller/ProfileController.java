// backend/src/main/java/com/example/AppGYM/controller/ProfileController.java
package com.example.AppGYM.controller;

import com.example.AppGYM.dto.ProfileDto;
import com.example.AppGYM.model.User;
import com.example.AppGYM.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository users;

    @GetMapping
    public ProfileDto get(@AuthenticationPrincipal User u) {
        return toDto(u);
    }

    @PutMapping @Transactional
    public void update(@AuthenticationPrincipal User u,
                       @RequestBody ProfileDto dto) {
        u.setFirstName(dto.firstName);
        u.setLastName(dto.lastName);
        u.setAge(dto.age);
        u.setHeightCm(dto.heightCm);
        u.setWeightKg(dto.weightKg);
        u.setNeckCm(dto.neckCm);
        u.setChestCm(dto.chestCm);
        u.setWaistCm(dto.waistCm);
        u.setLowerAbsCm(dto.lowerAbsCm);
        u.setHipCm(dto.hipCm);
        u.setBicepsCm(dto.bicepsCm);
        u.setBicepsFlexCm(dto.bicepsFlexCm);
        u.setForearmCm(dto.forearmCm);
        u.setThighCm(dto.thighCm);
        u.setCalfCm(dto.calfCm);
        u.setFrontImgUrl(dto.frontImgUrl);
        u.setSideImgUrl(dto.sideImgUrl);
        u.setBackImgUrl(dto.backImgUrl);
        users.save(u);
    }

    private static ProfileDto toDto(User u) {
        ProfileDto d=new ProfileDto();
        d.firstName=u.getFirstName(); d.lastName=u.getLastName();
        d.age=u.getAge(); d.heightCm=u.getHeightCm(); d.weightKg=u.getWeightKg();
        d.neckCm=u.getNeckCm(); d.chestCm=u.getChestCm(); d.waistCm=u.getWaistCm();
        d.lowerAbsCm=u.getLowerAbsCm(); d.hipCm=u.getHipCm();
        d.bicepsCm=u.getBicepsCm(); d.bicepsFlexCm=u.getBicepsFlexCm();
        d.forearmCm=u.getForearmCm(); d.thighCm=u.getThighCm(); d.calfCm=u.getCalfCm();
        d.frontImgUrl=u.getFrontImgUrl(); d.sideImgUrl=u.getSideImgUrl(); d.backImgUrl=u.getBackImgUrl();
        return d;
    }
}
