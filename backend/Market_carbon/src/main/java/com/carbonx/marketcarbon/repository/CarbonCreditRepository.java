package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.common.CreditStatus;
import com.carbonx.marketcarbon.model.CarbonCredit;
import com.carbonx.marketcarbon.model.Company;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CarbonCreditRepository extends JpaRepository<CarbonCredit, Long>,
        JpaSpecificationExecutor<CarbonCredit> {


    Optional<CarbonCredit> findByCompanyAndStatus(Company owner, CreditStatus status);

    Page<CarbonCredit> findByStatus(CreditStatus status, Pageable pageable);

    Optional<CarbonCredit> findByCreditCodeAndCompany_Id(String creditCode, Long companyId);

    Optional<CarbonCredit> findByCreditCode(String creditCode);

    List<CarbonCredit> findByCompanyId(Long companyId);

    List<CarbonCredit> findByStatusNot(CreditStatus status);

    List<CarbonCredit> findByBatch_IdAndCompany_Id(Long batchId, Long companyId);

    Optional<CarbonCredit> findFirstByCompanyAndStatus(Company company, CreditStatus status);

    @Query("""
        SELECT c.status, COALESCE(SUM(c.amount), 0)
        FROM CarbonCredit c
        WHERE c.company.id = :companyId
        GROUP BY c.status
    """)
    List<Object[]> sumAmountByStatus(@Param("companyId") Long companyId);

    @Query("""
        SELECT c.project.id, c.project.title, COALESCE(SUM(c.amount), 0)
        FROM CarbonCredit c
        WHERE c.company.id = :companyId
        GROUP BY c.project.id, c.project.title
    """)
    List<Object[]> sumAmountByProject(@Param("companyId") Long companyId);

    @Query("""
        SELECT COALESCE(c.vintageYear, 0), COALESCE(SUM(c.amount), 0)
        FROM CarbonCredit c
        WHERE c.company.id = :companyId
        GROUP BY COALESCE(c.vintageYear, 0)
    """)
    List<Object[]> sumAmountByVintage(@Param("companyId") Long companyId);


    Optional<CarbonCredit> findByStatus(CreditStatus status);

    @Query("""
        SELECT c.status, COALESCE(SUM(c.amount), 0)
        FROM CarbonCredit c
        WHERE c.company.id = :companyId
          AND c.status <> :excludedStatus
        GROUP BY c.status
    """)
    List<Object[]> sumAmountByStatusExcluding(@Param("companyId") Long companyId,
                                              @Param("excludedStatus") CreditStatus excludedStatus);

    @Query("""
        SELECT c.project.id, c.project.title, COALESCE(SUM(c.amount), 0)
        FROM CarbonCredit c
        WHERE c.company.id = :companyId
          AND c.status <> :excludedStatus
        GROUP BY c.project.id, c.project.title
    """)
    List<Object[]> sumAmountByProjectExcluding(@Param("companyId") Long companyId,
                                               @Param("excludedStatus") CreditStatus excludedStatus);

    @Query("""
        SELECT COALESCE(c.vintageYear, 0), COALESCE(SUM(c.amount), 0)
        FROM CarbonCredit c
        WHERE c.company.id = :companyId
          AND c.status <> :excludedStatus
        GROUP BY COALESCE(c.vintageYear, 0)
    """)
    List<Object[]> sumAmountByVintageExcluding(@Param("companyId") Long companyId,
                                               @Param("excludedStatus") CreditStatus excludedStatus);


    @Query("""
        SELECT COALESCE(SUM(c.amount), 0)
        FROM CarbonCredit c
        WHERE c.company.id = :companyId
          AND c.status = :status
    """)
    long sumAmountByCompany_IdAndStatus(@Param("companyId") Long companyId,
                                        @Param("status") CreditStatus status);

    @Query("""
        SELECT COALESCE(SUM(c.amount), 0)
        FROM CarbonCredit c
        WHERE c.company.id = :companyId
          AND c.status <> :status
    """)
    long sumAmountByCompany_IdAndStatusNot(@Param("companyId") Long companyId,
                                           @Param("status") CreditStatus status);

    long countByCompany_IdAndStatusNot(Long companyId, CreditStatus status);


    @Modifying
    @Query("""
        UPDATE CarbonCredit c
        SET c.status = 'EXPIRED'
        WHERE c.expiryDate < CURRENT_DATE
          AND c.status <> 'EXPIRED'
    """)
    int markExpiredCredits();

    @Query("""
    SELECT COALESCE(SUM(c.amount), 0)
    FROM CarbonCredit c
    WHERE c.company.id = :companyId
      AND c.issuedAt >= :cutoffDate
      AND c.status <> 'EXPIRED'
""")
    long sumRecentlyIssued(@Param("companyId") Long companyId,
                           @Param("cutoffDate") java.time.OffsetDateTime cutoffDate);

    // Phương thức tìm kiếm với khóa pessimistic
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CarbonCredit c WHERE c.id = :id")
    Optional<CarbonCredit> findByIdWithPessimisticLock(@Param("id") Long id);

    // Tìm trực tiếp theo ID và companyId
    Optional<CarbonCredit> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * Tìm CarbonCredit theo ID và Company ID đồng thời khóa PESSIMISTIC_WRITE.
     * Phương thức này dùng cho các hoạt động cần cập nhật an toàn (như retire, list).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE) // tránh race condition khi có 2 giao dịch cùng “retire” 1 credit.
    @Query("SELECT c FROM CarbonCredit c WHERE c.id = :id AND c.company.id = :companyId")
    Optional<CarbonCredit> findByIdAndCompanyIdWithLock(@Param("id") Long id, @Param("companyId") Long companyId);

    // Query tối ưu để tìm credit phù hợp
    @Query(nativeQuery = true, value =
            "SELECT c.* FROM carbon_credits c " +
                    "LEFT JOIN credit_batches b ON c.batch_id = b.id " +
                    "WHERE (c.company_id = :companyId) AND " +
                    "((c.id = :creditId) OR (c.source_credit_id = :creditId) OR (b.id = :creditId)) AND " +
                    "((c.carbon_credit >= :requiredAmount) OR (:requiredAmount IS NULL)) " +
                    "ORDER BY c.carbon_credit DESC LIMIT 1")
    List<CarbonCredit> findCreditsBatchOrChainLinkedToIdWithSufficientAmount(
            @Param("creditId") Long creditId,
            @Param("companyId") Long companyId,
            @Param("requiredAmount") BigDecimal requiredAmount);

    // AVAILABLE có nguồn gốc "mua qua sàn"
    @Query("""
    SELECT COALESCE(SUM(c.amount), 0)
    FROM CarbonCredit c
    WHERE c.company.id = :companyId
      AND c.status = 'AVAILABLE'
      AND EXISTS (
          SELECT 1
          FROM Order o
          WHERE o.carbonCredit = c
            AND o.company.id = :companyId
            AND o.orderType = 'BUY'
            AND o.orderStatus = 'COMPLETED'
      )
""")
    long sumAvailablePurchased(@Param("companyId") Long companyId);

    // AVAILABLE có nguồn gốc "phát hành nội bộ/nhận nội bộ"
    @Query("""
    SELECT COALESCE(SUM(c.amount), 0)
    FROM CarbonCredit c
    WHERE c.company.id = :companyId
      AND c.status = 'AVAILABLE'
      AND (
           c.sourceCredit IS NULL
        OR c.sourceCredit.company.id = :companyId
      )
""")
    long sumAvailableIssued(@Param("companyId") Long companyId);

    // tìm cả tín chỉ mua từ marketplace
    @Query("SELECT c FROM CarbonCredit c WHERE " +
            "(c.batch.id = :batchId AND c.company.id = :companyId) " +
            "OR EXISTS (SELECT t FROM WalletTransaction t WHERE " +
            "t.wallet.company.id = :companyId AND " +
            "t.order.carbonCredit.batch.id = :batchId AND " +
            "t.transactionType = 'BUY_CARBON_CREDIT')")
    List<CarbonCredit> findAllOwnedByBatch(@Param("batchId") Long batchId, @Param("companyId") Long companyId);


    /**
     * Tìm credits THUỘC BATCH GỐC (không phải credits đã mua)
     */
    @Query("""
    SELECT c FROM CarbonCredit c
    WHERE c.batch.id = :batchId
    AND c.company.id = :companyId
    AND c.sourceCredit IS NULL
    """)
    List<CarbonCredit> findOriginalCreditsByBatchAndCompany(
            @Param("batchId") Long batchId,
            @Param("companyId") Long companyId
    );

    @Query("""
        SELECT COUNT(c.id)
        FROM CarbonCredit c
        WHERE c.company.id = :companyId
          AND c.status = :status
    """)
    long countByCompanyIdAndStatus(@Param("companyId") Long companyId,
                                   @Param("status") CreditStatus status);

}
