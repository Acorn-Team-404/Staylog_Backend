package com.staylog.staylog.domain.payment.mapper;

import com.staylog.staylog.domain.payment.entity.CompensationFailure;
import org.apache.ibatis.annotations.Mapper;

/**
 * DLQ (Dead Letter Queue) Mapper
 * 보상 트랜잭션 실패 이력 관리
 */
@Mapper
public interface CompensationFailureMapper {

    /**
     * DLQ에 저장
     */
    void insert(CompensationFailure failure);
}
