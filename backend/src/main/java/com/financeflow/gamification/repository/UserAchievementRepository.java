package com.financeflow.gamification.repository;

import com.financeflow.gamification.domain.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, UUID> {

    @Query("SELECT ua FROM UserAchievement ua WHERE ua.user.id = :userId ORDER BY ua.unlockedAt DESC")
    List<UserAchievement> findAllByUserId(@Param("userId") UUID userId);

    @Query("SELECT ua FROM UserAchievement ua WHERE ua.user.id = :userId AND ua.achievementCode = :code")
    Optional<UserAchievement> findByUserIdAndAchievementCode(@Param("userId") UUID userId, @Param("code") String code);

    @Query("SELECT COUNT(ua) > 0 FROM UserAchievement ua WHERE ua.user.id = :userId AND ua.achievementCode = :code")
    boolean existsByUserIdAndAchievementCode(@Param("userId") UUID userId, @Param("code") String code);
}
