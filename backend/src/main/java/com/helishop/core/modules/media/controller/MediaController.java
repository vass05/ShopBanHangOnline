package com.helishop.core.modules.media.controller;

import com.helishop.core.common.response.ApiResponse;
import com.helishop.core.modules.media.dto.MediaUploadResponse;
import com.helishop.core.modules.media.service.MediaUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Tag(name = "Media Management", description = "APIs tải lên và quản lý hình ảnh sản phẩm chuẩn tỉ lệ 1:1 catalog Shopee")
public class MediaController {

    private final MediaUploadService mediaUploadService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Tải lên 1 tệp hình ảnh sản phẩm",
            description = "Hỗ trợ định dạng image/jpeg, image/png, image/webp, kích thước tối đa 5MB. Tự động chuyển đổi tỉ lệ vuông 1:1 (800x800) và tối ưu định dạng CDN"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Tải ảnh lên thành công",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Định dạng không được hỗ trợ hoặc dung lượng vượt quá 5MB",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực danh tính",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<MediaUploadResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        MediaUploadResponse response = mediaUploadService.uploadImage(file);
        return ApiResponse.success(response, "Tải ảnh lên Cloudinary thành công");
    }

    @PostMapping(value = "/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Tải lên nhiều hình ảnh sản phẩm cùng lúc",
            description = "Tải lên danh sách các tệp hình ảnh sản phẩm, tự động crop tỉ lệ 1:1 và trả về danh sách URL CDN an toàn"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Tải các ảnh lên thành công",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Tệp tin không hợp lệ",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực danh tính",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<List<MediaUploadResponse>> uploadMultipleImages(@RequestParam("files") List<MultipartFile> files) {
        List<MediaUploadResponse> responses = mediaUploadService.uploadMultipleImages(files);
        return ApiResponse.success(responses, "Tải danh sách ảnh lên Cloudinary thành công");
    }
}
