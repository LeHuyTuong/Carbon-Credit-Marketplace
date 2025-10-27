package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.common.CreditStatus;
import com.carbonx.marketcarbon.model.CarbonCredit;
import com.carbonx.marketcarbon.model.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CarbonCreditRepository extends JpaRepository<CarbonCredit, Long>,
        JpaSpecificationExecutor<CarbonCredit> {

    Optional<CarbonCredit> findByCompanyAndStatus(Company owner, CreditStatus status);
    Page<CarbonCredit> findByStatus(CreditStatus status, Pageable pageable);
    Optional<CarbonCredit> findByCreditCodeAndCompany_Id(String creditCode, Long companyId);
    Optional<CarbonCredit> findByCreditCode(String creditCode);
    Optional<CarbonCredit> findFirstByCompanyAndStatus(Company company, CreditStatus status);

    List<CarbonCredit> findByCompanyId(Long companyId);

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

    @Query("""
        SELECT COALESCE(SUM(c.amount), 0)
        FROM CarbonCredit c
        WHERE c.company.id = :companyId
          AND c.status = :status
    """)
    long sumAmountByCompany_IdAndStatus(@Param("companyId") Long companyId,
                                        @Param("status") CreditStatus status);

    // --- Tổng hợp loại trừ trạng thái ---
    @Query("""
        SELECT c.status, COALESCE(SUM(c.amount), 0)
        FROM CarbonCredit c
        WHERE c.company.id = :companyId
          AND c.status <> :excludedStatus
        GROUP BY c.status
    """)
    List<Object[]> sumAmountByStatusExcluding(@Param("companyId") Long companyId,
                                              @Param("excludedStatus") String excludedStatus);

    @Query("""
        SELECT c.project.id, c.project.title, COALESCE(SUM(c.amount), 0)
        FROM CarbonCredit c
        WHERE c.company.id = :companyId
          AND c.status <> :excludedStatus
        GROUP BY c.project.id, c.project.title
    """)
    List<Object[]> sumAmountByProjectExcluding(@Param("companyId") Long companyId,
                                               @Param("excludedStatus") String excludedStatus);

    @Query("""
        SELECT COALESCE(c.vintageYear, 0), COALESCE(SUM(c.amount), 0)
        FROM CarbonCredit c
        WHERE c.company.id = :companyId
          AND c.status <> :excludedStatus
        GROUP BY COALESCE(c.vintageYear, 0)
    """)
    List<Object[]> sumAmountByVintageExcluding(@Param("companyId") Long companyId,
                                               @Param("excludedStatus") String excludedStatus);

    // --- Tiện ích phụ trợ ---
    List<CarbonCredit> findByStatusNot(CreditStatus status);

    long countByCompany_IdAndStatusNot(Long companyId, CreditStatus status);

    @Query("""
        SELECT COALESCE(SUM(c.amount), 0)
        FROM CarbonCredit c
        WHERE c.company.id = :companyId
          AND c.status <> :status
    """)
    long sumAmountByCompany_IdAndStatusNot(@Param("companyId") Long companyId,
                                           @Param("status") CreditStatus status);

    @Modifying
    @Query("UPDATE CarbonCredit c SET c.status='EXPIRED' WHERE c.expiryDate < CURRENT_DATE AND c.status <> 'EXPIRED'")
    int markExpiredCredits();



}
