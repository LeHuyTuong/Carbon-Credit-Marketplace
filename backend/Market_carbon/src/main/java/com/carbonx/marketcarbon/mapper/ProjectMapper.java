package com.carbonx.marketcarbon.mapper;

import com.carbonx.marketcarbon.dto.response.ProjectResponse;
import com.carbonx.marketcarbon.model.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;


@Mapper(componentModel = "spring")
public interface ProjectMapper {

    @Mapping(source = "company.companyName", target = "companyName")
    @Mapping(target = "reviewer", expression = "java(project.getReviewer() != null ? project.getReviewer().getName() : null)")
    @Mapping(target = "finalReviewer", expression = "java(project.getFinalReviewer() != null ? project.getFinalReviewer().getName() : null)")
    ProjectResponse toResponse(Project project);

}
