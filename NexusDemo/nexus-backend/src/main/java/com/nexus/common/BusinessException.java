package com.nexus.common;

/**
 * 业务异常
 */
public class BusinessException extends RuntimeException {
    private Integer code;

    public BusinessException() {
        super(ApiResult.ERROR.getMessage());
        this.code = ApiResult.ERROR.getCode();
    }

    public BusinessException(String message) {
        super(message);
        this.code = ApiResult.ERROR.getCode();
    }

    public BusinessException(ApiResult apiResult) {
        super(apiResult.getMessage());
        this.code = apiResult.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = ApiResult.ERROR.getCode();
    }

    public Integer getCode() {
        return code;
    }
}