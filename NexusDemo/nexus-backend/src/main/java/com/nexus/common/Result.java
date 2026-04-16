package com.nexus.common;

import lombok.Data;
import java.io.Serializable;

/**
 * 统一响应结果
 */
@Data
public class Result<T> implements Serializable {
    private Integer code;
    private String message;
    private T data;

    public Result() {
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ApiResult.SUCCESS.getCode(), ApiResult.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ApiResult.SUCCESS.getCode(), message, data);
    }

    public static <T> Result<T> error() {
        return error(ApiResult.ERROR);
    }

    public static <T> Result<T> error(ApiResult apiResult) {
        return new Result<>(apiResult.getCode(), apiResult.getMessage(), null);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(ApiResult.ERROR.getCode(), message, null);
    }
}