package com.staylog.staylog.global.exception.custom;

import com.staylog.staylog.global.common.code.ErrorCode;
import com.staylog.staylog.global.exception.BusinessException;

/**
 * 인증되지 않은 사용자가 접근 시 발생하는 예외
 * HTTP 401 Unauthorized
 */
public class UnauthorizedException extends BusinessException {

    /**
     * ErrorCode로 예외 생성 (권장)
     *
     * @param errorCode 에러 코드
     */
    public UnauthorizedException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * ErrorCode와 추가 메시지로 예외 생성
     *
     * @param errorCode 에러 코드
     * @param detail 추가 상세 메시지
     */
    public UnauthorizedException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
