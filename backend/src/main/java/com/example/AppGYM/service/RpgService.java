// backend/src/main/java/com/example/AppGYM/service/RpgService.java
package com.example.AppGYM.service;

import com.example.AppGYM.model.RpgCharacter;
import com.example.AppGYM.model.User;
import com.example.AppGYM.repository.RpgCharacterRepository;
import com.example.AppGYM.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RpgService {

    private final RpgCharacterRepository pjRepo;
    private final UserRepository         userRepo;

    public RpgService(RpgCharacterRepository pjRepo, UserRepository userRepo) {
        this.pjRepo  = pjRepo;
        this.userRepo = userRepo;
    }

    /* --------- ficha existente o nueva --------- */
    @Transactional
    public RpgCharacter getOrCreate(String username) {
        User user = userRepo.findByEmail(username)   // adapta si tu campo se llama distinto
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));

        return pjRepo.findByUserId(user.getId())
                .orElseGet(() -> {
                    RpgCharacter pj = new RpgCharacter();
                    pj.setUser(user);
                    pj.setClazz("Explorador");
                    return pjRepo.save(pj);
                });
    }

    /* --------- aplica deltas y guarda ---------- */
    @Transactional
    public RpgCharacter applyDelta(String username,
                                   int dStr, int dEng, int dVit, int dWis, int dXp) {

        RpgCharacter pj = getOrCreate(username);

        pj.setStr(pj.getStr() + dStr);
        pj.setEng(pj.getEng() + dEng);
        pj.setVit(pj.getVit() + dVit);
        pj.setWis(pj.getWis() + dWis);

        int xp      = pj.getXp() + dXp;
        int level   = pj.getLevel();
        int xpNext  = pj.getXpNext();
        while (xp >= xpNext) {
            xp -= xpNext;
            level++;
            xpNext = (int)(xpNext * 1.25);
        }
        pj.setXp(xp);
        pj.setLevel(level);
        pj.setXpNext(xpNext);

        return pjRepo.save(pj);
    }
}
