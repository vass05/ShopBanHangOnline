package com.helishop.core.media;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.helishop.core.common.exception.AppException;
import com.helishop.core.common.exception.ErrorCode;
import com.helishop.core.modules.media.dto.MediaUploadResponse;
import com.helishop.core.modules.media.service.MediaUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaUploadServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    private MediaUploadService mediaUploadService;

    @BeforeEach
    void setUp() {
        mediaUploadService = new MediaUploadService(cloudinary);
    }

    @Test
    @DisplayName("Upload hình ảnh thành công, tự động chuẩn hóa tỉ lệ vuông 1:1 và trả về HTTPS URL")
    void testUploadImage_Success() throws Exception {
        byte[] content = "fake-image-binary-data".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "product-image.jpg",
                "image/jpeg",
                content
        );

        when(cloudinary.uploader()).thenReturn(uploader);

        Map<String, Object> result = new HashMap<>();
        result.put("secure_url", "https://res.cloudinary.com/helishop/image/upload/v1/product.webp");
        result.put("public_id", "helishop/products/sample_product");
        result.put("format", "webp");
        result.put("width", 800);
        result.put("height", 800);
        result.put("bytes", 1024L);

        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(result);

        MediaUploadResponse response = mediaUploadService.uploadImage(file);

        assertThat(response).isNotNull();
        assertThat(response.getUrl()).isEqualTo("https://res.cloudinary.com/helishop/image/upload/v1/product.webp");
        assertThat(response.getWidth()).isEqualTo(800);
        assertThat(response.getHeight()).isEqualTo(800);
        assertThat(response.getFormat()).isEqualTo("webp");
    }

    @Test
    @DisplayName("File rỗng -> Báo lỗi BAD_REQUEST")
    void testValidateFile_EmptyFile_ThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.png",
                "image/png",
                new byte[0]
        );

        assertThatThrownBy(() -> mediaUploadService.validateFile(emptyFile))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("Kích thước file vượt quá 5MB -> Báo lỗi BAD_REQUEST")
    void testValidateFile_ExceedSize_ThrowsException() {
        // 6 MB
        byte[] largeContent = new byte[6 * 1024 * 1024];
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "large.jpg",
                "image/jpeg",
                largeContent
        );

        assertThatThrownBy(() -> mediaUploadService.validateFile(largeFile))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("Định dạng file không được hỗ trợ (PDF, TXT) -> Báo lỗi BAD_REQUEST")
    void testValidateFile_UnsupportedFormat_ThrowsException() {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "pdf-data".getBytes()
        );

        assertThatThrownBy(() -> mediaUploadService.validateFile(pdfFile))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BAD_REQUEST);
    }
}
