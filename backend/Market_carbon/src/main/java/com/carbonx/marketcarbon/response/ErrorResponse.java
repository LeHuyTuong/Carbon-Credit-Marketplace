package com.carbonx.marketcarbon.response;


// lỗi do ko gửi noti
public class ErrorResponse extends ApiResponse{
    public ErrorResponse(boolean success, String error) {
        super(success,error);
    }
}
