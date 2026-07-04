package com.financeflow.gamification.repository;

import com.financeflow.gamification.domain.UserStreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserStreakRepository extends JpaRepository<UserStreak, UUID> {

    @Query("SELECT s FROM UserStreak s WHERE s.user.id = :userId")
    Optional<UserStreak> findByUserId(@Param("userId") UUID userId);
}
