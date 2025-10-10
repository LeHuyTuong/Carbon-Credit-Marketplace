package com.carbonx.marketcarbon.mapper;

import com.carbonx.marketcarbon.dto.response.ProjectResponse;
import com.carbonx.marketcarbon.model.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;


@Mapper(componentModel = "spring")
public interface ProjectMapper {

    @Mapping(source = "company.companyName", target = "companyName")
    ProjectResponse toResponse(Project project);

}
