package com.travel.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 文件上传工具。
 * 作用：
 * 1. 统一处理扩展名白名单校验；
 * 2. 统一落盘目录策略（按日期分目录）；
 * 3. 在目标目录不可写时提供安全回退，避免接口直接 500。
 */
@Component
public class FileUploadUtils {

    @Value("${upload.path}")
    private String uploadPath;

    /**
     * 允许上传的文件类型白名单。
     */
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".pdf"
    );

    /**
     * 保存文件并返回可访问路径。
     *
     * @param file 上传文件
     * @return 形如 /uploads/yyyy/MM/dd/xxx.png 的访问路径
     * @throws IOException 文件写入失败
     */
    public String upload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String safeFilename = originalFilename == null ? "" : new File(originalFilename).getName();
        int dotIndex = safeFilename.lastIndexOf(".");
        if (dotIndex < 0) {
            throw new RuntimeException("文件名缺少后缀");
        }

        String extension = safeFilename.substring(dotIndex).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new RuntimeException("不支持的文件类型，仅允许: jpg, jpeg, png, gif, webp, pdf");
        }

        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;

        File baseDir = resolveBaseDir();
        String datePath = new java.text.SimpleDateFormat("yyyy/MM/dd").format(new java.util.Date());
        File dateDir = new File(baseDir, datePath);
        if (!dateDir.exists() && !dateDir.mkdirs()) {
            throw new RuntimeException("无法创建上传子目录: " + datePath);
        }

        File targetFile = new File(dateDir, fileName);
        file.transferTo(targetFile);

        Path basePath = baseDir.toPath().toAbsolutePath().normalize();
        Path targetPath = targetFile.toPath().toAbsolutePath().normalize();
        String relative = basePath.relativize(targetPath).toString().replace("\\", "/");
        return "/uploads/" + relative;
    }

    /**
     * 解析上传根目录。
     * 优先使用配置目录；若不可写则回退到系统临时目录。
     */
    private File resolveBaseDir() {
        File baseDir = new File(uploadPath);
        if (!baseDir.isAbsolute()) {
            baseDir = new File(System.getProperty("user.dir"), uploadPath);
        }
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            baseDir = new File(System.getProperty("java.io.tmpdir"), "travel-uploads");
            if (!baseDir.exists() && !baseDir.mkdirs()) {
                throw new RuntimeException("无法创建上传根目录");
            }
        }
        return baseDir;
    }
}
