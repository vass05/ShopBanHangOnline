package com.helishop.core.modules.media.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import com.helishop.core.common.exception.AppException;
import com.helishop.core.common.exception.ErrorCode;
import com.helishop.core.modules.media.dto.MediaUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaUploadService {

    public static final long MAX_FILE_SIZE = 5 * 1024 * 1024L; // 5 MB
    public static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final Cloudinary cloudinary;

    /**
     * Tải lên một hình ảnh lên Cloudinary, tự động chuẩn hóa tỉ lệ vuông 1:1 (800x800) chuẩn Shopee
     */
    public MediaUploadResponse uploadImage(MultipartFile file) {
        validateFile(file);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadOptions = ObjectUtils.asMap(
                    "folder", "helishop/products",
                    "resource_type", "image",
                    "transformation", new Transformation<>()
                            .width(800)
                            .height(800)
                            .crop("fill")
                            .gravity("center")
                            .fetchFormat("auto")
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadOptions);

            String secureUrl = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");
            String format = (String) uploadResult.get("format");
            Integer width = (Integer) uploadResult.get("width");
            Integer height = (Integer) uploadResult.get("height");
            Long bytes = uploadResult.get("bytes") instanceof Number n ? n.longValue() : file.getSize();

            log.info("Tải ảnh lên Cloudinary thành công! URL: '{}', PublicId: '{}'", secureUrl, publicId);

            return MediaUploadResponse.builder()
                    .url(secureUrl)
                    .publicId(publicId)
                    .format(format)
                    .width(width)
                    .height(height)
                    .size(bytes)
                    .build();
        } catch (IOException e) {
            log.error("Lỗi khi upload ảnh lên Cloudinary: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Không thể tải ảnh lên dịch vụ Cloudinary: " + e.getMessage());
        }
    }

    /**
     * Tải lên danh sách nhiều hình ảnh sản phẩm
     */
    public List<MediaUploadResponse> uploadMultipleImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Danh sách tệp tin tải lên không được để trống");
        }

        List<MediaUploadResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            responses.add(uploadImage(file));
        }
        return responses;
    }

    /**
     * Xác minh tính hợp lệ của tệp ảnh trước khi upload
     */
    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Tệp tin tải lên không được để trống");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new AppException(ErrorCode.BAD_REQUEST,
                    String.format("Kích thước tệp (%.2f MB) vượt quá giới hạn tối đa cho phép (5.00 MB)",
                            file.getSize() / (1024.0 * 1024.0)));
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new AppException(ErrorCode.BAD_REQUEST,
                    String.format("Định dạng tệp '%s' không được hỗ trợ. Chỉ chấp nhận các định dạng: image/jpeg, image/png, image/webp",
                            contentType));
        }
    }
}
