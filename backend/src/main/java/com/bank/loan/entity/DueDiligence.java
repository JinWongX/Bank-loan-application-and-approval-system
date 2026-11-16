package com.bank.loan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("due_diligence")
public class DueDiligence {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long applicationId;

    private Long managerId;

    private String businessSituation;

    private String assetsLiabilities;

    private String repaymentAbility;

    private String conclusion;

    private String attachmentPath;

    private LocalDateTime submitTime;
}
