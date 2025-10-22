package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.dto.request.ProjectApplicationRequest;
import com.carbonx.marketcarbon.dto.response.ProjectApplicationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProjectApplicationService {

    // Company nộp đơn -> status = UNDER_REVIEW (gửi qua CVA)
    ProjectApplicationResponse submit(Long projectId, MultipartFile file);

    // CVA duyệt hoặc từ chối -> CVA_APPROVED / CVA_REJECTED
    public ProjectApplicationResponse cvaDecision(Long applicationId, boolean approved, String note);

    // Admin duyệt cuối hoặc từ chối -> APPROVED / REJECTED (chỉ khi đã CVA_APPROVED)
    public ProjectApplicationResponse adminFinalDecision(Long applicationId, boolean approved, String note);

    public List<ProjectApplicationResponse> listMyApplications(String status);



    // Tiện ích quản trị
    List<ProjectApplicationResponse> listAll();
    ProjectApplicationResponse getById(Long id);
    Page<ProjectApplicationResponse> listCvaApprovedApplications(Pageable pageable);
    List<ProjectApplicationResponse> listPendingForCva();

}
