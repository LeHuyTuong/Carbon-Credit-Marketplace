package com.carbonx.marketcarbon.service.credit;

import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.CreditSerialCounter;
import com.carbonx.marketcarbon.model.Project;
import com.carbonx.marketcarbon.repository.CreditSerialCounterRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SerialNumberService {
    private final CreditSerialCounterRepository counterRepo;

    @Transactional
    public SerialRange allocate(Project p, Company c, int year, int count) {
        var ctr = counterRepo.lockBy(year, p, c)
                .orElseGet(() -> counterRepo.save(
                        CreditSerialCounter.builder()
                                .vintageYear(year)
                                .project(p)
                                .company(c)
                                .nextSerial(1L)
                                .build()
                ));
        long from = ctr.getNextSerial();
        long to = from + count - 1;
        ctr.setNextSerial(to + 1);
        counterRepo.save(ctr);
        return new SerialRange(from, to);
    }

    public String buildCode(int year, String companyCode, String projectCode, long serial) {
        return year + "-" + companyCode + "-" + projectCode + "-" + String.format("%06d", serial);
    }

    public record SerialRange(long from, long to) {}
}
