package com.bank.loan.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApplicationMaterialDTO {

    private Long id;

    private Long applicationId;

    private String materialType;

    private String fileName;

    private String filePath;

    private LocalDateTime uploadTime;
}
