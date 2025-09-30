package com.carbonx.marketcarbon.repository;

import com.carbonx.marketcarbon.dto.response.PageResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SearchRepository {

    @PersistenceContext
    private EntityManager em;

    public PageResponse<?> getAllVehiclesWithSortByMultipleColumnsAndSearch(int pageNo, int pageSize, String search, String sortBy) {
        //B1 query list vehicle
        // sử dụng alias , mật danh trong JPA
        StringBuilder sqlQuery = new StringBuilder("select new com.carbonx.marketcarbon.dto.response.VehicleDetailResponse(v.id, v.plateNumber, v.brand, v.model, v.yearOfManufacture, v.createAt, v.updatedAt)" +
                " from Vehicle v where 1=1");           // package

        if(StringUtils.hasLength(search)){
            sqlQuery.append(" and (lower(v.plateNumber) like lower(:plateNumber))")
                    .append(" and (lower(v.model) like lower(:model))")
                    .append(" and (lower(v.brand) like lower(:brand))");
        }

        if(StringUtils.hasLength(search)){
            //firstName : asc|desc
            Pattern pattern = Pattern.compile("(\\w+)[,:](asc|desc)?",  Pattern.CASE_INSENSITIVE);
            // co 3 group
            Matcher matcher = pattern.matcher(search);

            if(matcher.find()){
                // xử lý sort
                String col = matcher.group(1);
                String dir = matcher.group(2) == null ? "asc" : matcher.group(2).toLowerCase();
                sqlQuery.append(String.format(" order by v.%s %s", col, dir));
            }
        }

        Query selectQuery = em.createQuery(sqlQuery.toString());

        if(StringUtils.hasLength(search)){
            String kw = "%" + search + "%";
            selectQuery.setParameter("plateNumber", kw);
            selectQuery.setParameter("brand", kw);
            selectQuery.setParameter("model", kw);
        }

        selectQuery.setFirstResult(pageNo); // vị trí của records. of set trong câu lệnh query, setFirstResult sẽ là 100 , là pageN
        selectQuery.setMaxResults(pageSize); // truyền vào max records

        List<?> vehicles = selectQuery.getResultList();

        //B2 Query Record
        // sử dụng theo số vị trí

        StringBuilder sqlCountQuery = new StringBuilder("select count(*) from Vehicle v where 1=1");
        if(StringUtils.hasLength(search)){
            sqlCountQuery.append(" and (lower(v.plateNumber) like lower(?1)") // truyen fiend cua entity
                    .append( " or lower(v.brand) like lower(?2)")
                    .append( " or lower(v.model) like lower(?3)");
        }

        Query selectCountQuery = em.createQuery(sqlCountQuery.toString());

        if(StringUtils.hasLength(search)){
            String kw = "%" +  search + "%";
            selectCountQuery.setParameter(1,kw); // them vao tung vi tri trong cau sqlCountQuery kia
            selectCountQuery.setParameter(2,kw);
            selectCountQuery.setParameter(3,kw);
        }

        Long totalElements = (Long) selectCountQuery.getSingleResult();// in ra 1 gia tri
        System.out.println("totalElements: " + totalElements);

        Page<?> page = new PageImpl<>(vehicles, PageRequest.of(pageNo, pageSize), totalElements);

        return PageResponse.builder()
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalPages(page.getTotalPages()) // kieu long nen can parseInt
                .items(page.stream().toList())
                .build();
    }
}
