package com.financeflow.transfers.repository;

import com.financeflow.transfers.domain.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, UUID> {

    @Query("SELECT t FROM Transfer t JOIN FETCH t.originAccount JOIN FETCH t.destinationAccount " +
           "WHERE t.user.id = :userId ORDER BY t.transferDate DESC, t.createdAt DESC")
    List<Transfer> findAllByUserIdOrderByTransferDateDesc(@Param("userId") UUID userId);

    @Query("SELECT t FROM Transfer t JOIN FETCH t.originAccount JOIN FETCH t.destinationAccount " +
           "WHERE t.id = :id AND t.user.id = :userId")
    Optional<Transfer> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
}
