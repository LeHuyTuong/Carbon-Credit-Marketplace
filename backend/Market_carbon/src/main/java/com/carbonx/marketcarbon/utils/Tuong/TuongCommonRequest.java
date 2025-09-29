package com.carbonx.marketcarbon.utils.Tuong;


import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TuongCommonRequest<T> {
    private String requestTrace;
    private String requestDateTime;

    @NotNull(message = "data must not be null")
    @Valid
    @JsonProperty("data") // JSON key chính thức
    @JsonAlias({"requestParameters","requestParamters"}) // chấp nhận alias cũ/lỗi chính tả
    private T data;
}
