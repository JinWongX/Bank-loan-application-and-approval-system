package com.bank.loan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("application_material")
public class ApplicationMaterial {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long applicationId;

    private String materialType;

    private String fileName;

    private String filePath;

    private LocalDateTime uploadTime;
}
