package com.bank.loan.controller;

import com.bank.loan.common.Result;
import com.bank.loan.entity.ApplicationMaterial;
import com.bank.loan.entity.LoanApplication;
import com.bank.loan.mapper.ApplicationMaterialMapper;
import com.bank.loan.mapper.LoanApplicationMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api")
public class FileController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileController.class);

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024L;

    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "pdf"));
    private static final Set<String> ALLOWED_CONTENT_TYPES = new HashSet<>(Arrays.asList(
            "image/jpeg",
            "image/png",
            "application/pdf"
    ));

    private static final Map<String, byte[]> MAGIC_NUMBERS = new HashMap<>();

    static {
        MAGIC_NUMBERS.put("jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        MAGIC_NUMBERS.put("jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        MAGIC_NUMBERS.put("png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
        MAGIC_NUMBERS.put("pdf", new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D});
    }

    private static final Pattern SAVED_FILENAME_PATTERN =
            Pattern.compile("^[a-fA-F0-9]{32}\\.(jpg|jpeg|png|pdf)$");

    @Autowired
    private ApplicationMaterialMapper applicationMaterialMapper;

    @Autowired
    private LoanApplicationMapper loanApplicationMapper;

    @PostMapping("/upload")
    public Result<List<String>> upload(
            @RequestParam(value = "file", required = false) MultipartFile singleFile,
            @RequestParam(value = "files", required = false) MultipartFile[] multipleFiles,
            HttpServletRequest request) {
        List<MultipartFile> files = new ArrayList<>();
        if (singleFile != null && !singleFile.isEmpty()) {
            files.add(singleFile);
        }
        if (multipleFiles != null) {
            for (MultipartFile file : multipleFiles) {
                if (file != null && !file.isEmpty()) {
                    files.add(file);
                }
            }
        }
        if (files.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }

        String uploadDirPath = System.getProperty("user.dir") + File.separator + "uploads";
        File uploadDir = new File(uploadDirPath);
        if (!uploadDir.exists() && !uploadDir.mkdirs()) {
            LOGGER.error("创建上传目录失败: {}", uploadDirPath);
            return Result.error("创建上传目录失败");
        }

        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.getSize() > MAX_FILE_SIZE) {
                return Result.error("文件大小不能超过5MB");
            }

            String originalName = file.getOriginalFilename();
            String ext = getExtension(originalName).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(ext) || !ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
                return Result.error("仅支持 jpg、png、pdf 格式的文件");
            }

            try {
                if (!isValidMagicNumber(file, ext)) {
                    LOGGER.warn("文件魔数校验失败，可能是伪造类型: originalName={}", originalName);
                    return Result.error("文件类型与扩展名不一致，请上传真实有效的文件");
                }
            } catch (IOException e) {
                LOGGER.error("读取文件魔数失败: {}", originalName, e);
                return Result.error("文件读取失败");
            }

            String savedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            File savedFile = new File(uploadDir, savedName);
            try {
                file.transferTo(savedFile);
            } catch (IOException e) {
                LOGGER.error("文件保存失败: {}", savedFile.getAbsolutePath(), e);
                return Result.error("文件上传失败");
            }

            String serverUrl = request.getScheme() + "://" + request.getServerName()
                    + ":" + request.getServerPort();
            urls.add(serverUrl + "/api/download/" + savedName);
        }
        return Result.success(urls);
    }

    @GetMapping("/download/{filename}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> download(@PathVariable String filename,
                                              Authentication authentication) {
        if (!SAVED_FILENAME_PATTERN.matcher(filename).matches()) {
            return ResponseEntity.notFound().build();
        }

        String filePath = "/api/download/" + filename;
        ApplicationMaterial material = applicationMaterialMapper.selectOne(
                new QueryWrapper<ApplicationMaterial>().eq("file_path", filePath));
        if (material == null) {
            return ResponseEntity.notFound().build();
        }

        LoanApplication application = loanApplicationMapper.selectById(material.getApplicationId());
        if (application == null) {
            return ResponseEntity.notFound().build();
        }

        if (isCustomer(authentication)) {
            Long customerId = getCurrentUserId(authentication);
            if (customerId == null || !customerId.equals(application.getCustomerId())) {
                return ResponseEntity.status(403).build();
            }
        }

        File file = new File(System.getProperty("user.dir") + File.separator + "uploads", filename);
        if (!file.exists() || !file.isFile()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + sanitizeFileName(material.getFileName()) + "\"")
                .contentType(getMediaType(filename))
                .body(resource);
    }

    private boolean isValidMagicNumber(MultipartFile file, String ext) throws IOException {
        byte[] expected = MAGIC_NUMBERS.get(ext);
        if (expected == null || expected.length == 0) {
            return false;
        }
        byte[] header = new byte[expected.length];
        try (InputStream is = file.getInputStream()) {
            int read = is.read(header);
            if (read < expected.length) {
                return false;
            }
        }
        for (int i = 0; i < expected.length; i++) {
            if (header[i] != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private String getExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int index = filename.lastIndexOf(".");
        return index == -1 ? "" : filename.substring(index + 1);
    }

    private MediaType getMediaType(String filename) {
        String ext = getExtension(filename).toLowerCase();
        switch (ext) {
            case "jpg":
            case "jpeg":
                return MediaType.IMAGE_JPEG;
            case "png":
                return MediaType.IMAGE_PNG;
            case "pdf":
                return MediaType.APPLICATION_PDF;
            default:
                return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "download";
        }
        String safe = fileName.replaceAll("[\\x00-\\x1f\\x22\\x5c\\x7f]", "_");
        return safe.isEmpty() ? "download" : safe;
    }

    private Long getCurrentUserId(Authentication authentication) {
        Object details = authentication.getDetails();
        if (details instanceof Long) {
            return (Long) details;
        }
        if (details instanceof Number) {
            return ((Number) details).longValue();
        }
        return null;
    }

    private boolean isCustomer(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> "ROLE_CUSTOMER".equals(auth.getAuthority()));
    }
}
