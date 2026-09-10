package com.helishop.core.media;

import com.helishop.core.modules.auth.service.RefreshTokenService;
import com.helishop.core.modules.media.dto.MediaUploadResponse;
import com.helishop.core.modules.media.service.MediaUploadService;
import com.helishop.core.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MediaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @MockBean
    private MediaUploadService mediaUploadService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    private String customerToken;

    @BeforeEach
    void setUp() {
        customerToken = "Bearer " + jwtUtils.generateAccessToken("customer@gmail.com", 1L, "ROLE_CUSTOMER", "Nguyen Van A");
    }

    @Test
    @DisplayName("Upload ảnh: Chưa đăng nhập -> Trả về 401 Unauthorized")
    void shouldReturn401WhenAnonymousUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "fake-bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/media/upload").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Upload ảnh: Đã xác thực token -> Trả về 201 Created kèm URL CDN Cloudinary")
    void shouldUploadImageSuccessfullyWhenAuthenticated() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "product.jpg",
                "image/jpeg",
                "fake-bytes".getBytes()
        );

        MediaUploadResponse uploadResponse = MediaUploadResponse.builder()
                .url("https://res.cloudinary.com/helishop/image/upload/v1/product.webp")
                .publicId("helishop/products/sample")
                .format("webp")
                .width(800)
                .height(800)
                .size(1024L)
                .build();

        when(mediaUploadService.uploadImage(any())).thenReturn(uploadResponse);

        mockMvc.perform(multipart("/api/v1/media/upload")
                        .file(file)
                        .header("Authorization", customerToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.url").value("https://res.cloudinary.com/helishop/image/upload/v1/product.webp"))
                .andExpect(jsonPath("$.data.width").value(800))
                .andExpect(jsonPath("$.data.height").value(800));
    }
}
