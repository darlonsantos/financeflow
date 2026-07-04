package com.financeflow.currency.repository;

import com.financeflow.currency.domain.CurrencyRate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CurrencyRateRepository extends JpaRepository<CurrencyRate, UUID> {

    Optional<CurrencyRate> findFirstByFromCurrencyCodeAndToCurrencyCodeAndEffectiveAtLessThanEqualOrderByEffectiveAtDesc(
        String fromCurrencyCode, String toCurrencyCode, LocalDateTime asOf);

    List<CurrencyRate> findByFromCurrencyCodeAndToCurrencyCodeOrderByEffectiveAtDesc(String fromCode, String toCode);

    void deleteByFromCurrencyCodeAndToCurrencyCode(String fromCode, String toCode);

    Optional<CurrencyRate> findFirstByFromCurrencyCodeAndToCurrencyCodeOrderByEffectiveAtDesc(String fromCode, String toCode);

    List<CurrencyRate> findByFromCurrencyCodeAndToCurrencyCodeAndEffectiveAtBetweenOrderByEffectiveAtDesc(
        String fromCode, String toCode, LocalDateTime start, LocalDateTime end);

    Page<CurrencyRate> findByFromCurrencyCodeInAndToCurrencyCodeAndEffectiveAtBetweenOrderByEffectiveAtDesc(
        List<String> fromCodes, String toCode, LocalDateTime start, LocalDateTime end, Pageable pageable);

    boolean existsByFromCurrencyCodeAndToCurrencyCodeAndEffectiveAtBetween(
        String fromCode, String toCode, LocalDateTime start, LocalDateTime end);
}
