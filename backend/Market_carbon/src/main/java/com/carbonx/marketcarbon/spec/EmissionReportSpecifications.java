//package com.carbonx.marketcarbon.spec;
//
//import com.carbonx.marketcarbon.common.EmissionStatus;
//import com.carbonx.marketcarbon.dto.request.ReportFilter;
//import com.carbonx.marketcarbon.model.EmissionReport;
//import org.springframework.data.jpa.domain.Specification;
//import org.springframework.stereotype.Component;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.function.Predicate;
//
//import jakarta.persistence.criteria.Join;
//import jakarta.persistence.criteria.JoinType;
//
//
//@Component
//public class EmissionReportSpecifications {
//
//    public Specification<EmissionReport> filter(ReportFilter f) {
//        return (root, q, cb) -> {
//            List<Predicate> ps = new ArrayList<>();
//
//            if (f == null) return cb.conjunction();
//
//            // tránh inner-join ngầm; dùng LEFT để không loại bản ghi khi liên kết null
//            Join<Object, Object> seller = root.join("seller", JoinType.LEFT);
//            Join<Object, Object> vehicle = root.join("vehicle", JoinType.LEFT);
//
//            if (f.getPeriod() != null) {
//                ps.add(cb.equal(root.get("period"), f.getPeriod()));
//            }
//            if (f.getSellerId() != null) {
//                ps.add(cb.equal(seller.get("id"), f.getSellerId()));
//            }
//            if (f.getVehicleId() != null) {
//                ps.add(cb.equal(vehicle.get("id"), f.getVehicleId()));
//            }
//            if (f.getStatus() != null && !f.getStatus().isBlank()) {
//                // phòng Invalid enum
//                EmissionStatus st = EmissionStatus.valueOf(f.getStatus().trim().toUpperCase());
//                ps.add(cb.equal(root.get("status"), st));
//            }
//
//            return ps.isEmpty() ? cb.conjunction() : cb.and(ps.toArray(new Predicate[0]));
//        };
//    }
//}