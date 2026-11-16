package com.bank.loan;

import com.bank.loan.config.JwtConfig;
import com.bank.loan.entity.ApplicationMaterial;
import com.bank.loan.entity.LoanApplication;
import com.bank.loan.mapper.ApplicationMaterialMapper;
import com.bank.loan.mapper.LoanApplicationMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtConfig jwtConfig;

    @MockBean
    private ApplicationMaterialMapper applicationMaterialMapper;

    @MockBean
    private LoanApplicationMapper loanApplicationMapper;

    private Path tempFile;

    private String customerToken() {
        return jwtConfig.generateToken(1L, "customer1", "customer");
    }

    private String adminToken() {
        return jwtConfig.generateToken(7L, "admin", "admin");
    }

    @BeforeEach
    void setUp() throws Exception {
        File uploadDir = new File(System.getProperty("user.dir") + File.separator + "uploads");
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (tempFile != null) {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void upload_fakeContentType_shouldBeRejected() throws Exception {
        // 真实 JPEG 文件头，但声明为 application/pdf
        byte[] jpegHeader = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "test.pdf",
                "application/pdf",
                jpegHeader
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("文件类型与扩展名不一致，请上传真实有效的文件"));
    }

    @Test
    void upload_noFiles_shouldReturnError() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/upload")
                        .header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("请选择要上传的文件"));
    }

    @Test
    void upload_invalidExtension_shouldReturnError() throws Exception {
        byte[] data = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D};
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "test.txt",
                "text/plain",
                data
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("仅支持 jpg、png、pdf 格式的文件"));
    }

    @Test
    void upload_fileTooLarge_shouldReturnError() throws Exception {
        byte[] data = new byte[5 * 1024 * 1024 + 1];
        new java.util.Random().nextBytes(data);
        data[0] = (byte) 0xFF;
        data[1] = (byte) 0xD8;
        data[2] = (byte) 0xFF;
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "large.jpg",
                "image/jpeg",
                data
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("文件大小不能超过5MB"));
    }

    @Test
    void upload_validJpg_shouldSuccess() throws Exception {
        byte[] data = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                data
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0]").exists());
    }

    @Test
    void download_invalidFilename_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/download/test.jpg")
                        .header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void download_materialNotFound_shouldReturn404() throws Exception {
        String filename = UUID.randomUUID().toString().replace("-", "") + ".jpg";
        when(applicationMaterialMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

        mockMvc.perform(get("/api/download/" + filename)
                        .header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void download_customerNotOwn_shouldReturn403() throws Exception {
        String filename = UUID.randomUUID().toString().replace("-", "") + ".jpg";
        tempFile = new File(System.getProperty("user.dir") + File.separator + "uploads", filename).toPath();
        Files.write(tempFile, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

        ApplicationMaterial material = new ApplicationMaterial();
        material.setApplicationId(2L);
        material.setFileName("test.jpg");
        when(applicationMaterialMapper.selectOne(any(QueryWrapper.class))).thenReturn(material);

        LoanApplication application = new LoanApplication();
        application.setId(2L);
        application.setCustomerId(2L);
        when(loanApplicationMapper.selectById(2L)).thenReturn(application);

        mockMvc.perform(get("/api/download/" + filename)
                        .header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void download_customerOwn_shouldSuccess() throws Exception {
        String filename = UUID.randomUUID().toString().replace("-", "") + ".jpg";
        tempFile = new File(System.getProperty("user.dir") + File.separator + "uploads", filename).toPath();
        Files.write(tempFile, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

        ApplicationMaterial material = new ApplicationMaterial();
        material.setApplicationId(1L);
        material.setFileName("test.jpg");
        when(applicationMaterialMapper.selectOne(any(QueryWrapper.class))).thenReturn(material);

        LoanApplication application = new LoanApplication();
        application.setId(1L);
        application.setCustomerId(1L);
        when(loanApplicationMapper.selectById(1L)).thenReturn(application);

        mockMvc.perform(get("/api/download/" + filename)
                        .header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "inline; filename=\"test.jpg\""));
    }

    @Test
    void download_admin_shouldSuccess() throws Exception {
        String filename = UUID.randomUUID().toString().replace("-", "") + ".jpg";
        tempFile = new File(System.getProperty("user.dir") + File.separator + "uploads", filename).toPath();
        Files.write(tempFile, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

        ApplicationMaterial material = new ApplicationMaterial();
        material.setApplicationId(2L);
        material.setFileName("test.jpg");
        when(applicationMaterialMapper.selectOne(any(QueryWrapper.class))).thenReturn(material);

        LoanApplication application = new LoanApplication();
        application.setId(2L);
        application.setCustomerId(2L);
        when(loanApplicationMapper.selectById(2L)).thenReturn(application);

        mockMvc.perform(get("/api/download/" + filename)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk());
    }
}
