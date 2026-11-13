package com.helishop.core.modules.user.service;

import com.helishop.core.common.exception.AppException;
import com.helishop.core.common.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

@Component
@Slf4j
public class AvatarStorage {
    public String store(Long userId, MultipartFile file) {
        return store(userId, file, Path.of("uploads", "avatars"));
    }

    String store(Long userId, MultipartFile file, Path directory) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Vui lòng chọn ảnh JPEG, PNG hoặc WEBP");
        }
        if (file.getSize() > 5 * 1024 * 1024L) throw new AppException(ErrorCode.FILE_TOO_LARGE);
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            String extension = switch (type) {
                case "image/png" -> header.length >= 8 && Arrays.equals(Arrays.copyOf(header, 8),
                        new byte[]{(byte)137, 80, 78, 71, 13, 10, 26, 10}) ? ".png" : null;
                case "image/jpeg" -> header.length >= 3 && header[0] == (byte)255 && header[1] == (byte)216
                        && header[2] == (byte)255 ? ".jpg" : null;
                case "image/webp" -> header.length == 12 && new String(header, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")
                        && new String(header, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP") ? ".webp" : null;
                default -> null;
            };
            if (extension == null) throw new AppException(ErrorCode.INVALID_REQUEST, "Nội dung ảnh phải đúng định dạng JPEG, PNG hoặc WEBP");
            Files.createDirectories(directory);
            String name = "user_" + userId + "_" + UUID.randomUUID() + extension;
            // A server-generated filename and verified media type prevent original-name traversal and active extensions.
            try (var output = Files.newOutputStream(directory.resolve(name))) {
                output.write(header);
                input.transferTo(output);
            }
            return "/uploads/avatars/" + name;
        } catch (IOException e) {
            log.error("Cannot store avatar for user {}", userId, e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Không thể lưu ảnh đại diện");
        }
    }
}
