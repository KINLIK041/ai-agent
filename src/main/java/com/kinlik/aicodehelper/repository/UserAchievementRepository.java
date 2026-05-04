package com.kinlik.aicodehelper.repository;

import com.kinlik.aicodehelper.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    List<UserAchievement> findByUsernameOrderByUnlockedAtDesc(String username);
    Optional<UserAchievement> findByUsernameAndBadgeId(String username, String badgeId);
    boolean existsByUsernameAndBadgeId(String username, String badgeId);
}
