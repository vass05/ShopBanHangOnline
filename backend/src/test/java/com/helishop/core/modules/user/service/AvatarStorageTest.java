package com.helishop.core.modules.user.service;

import com.helishop.core.common.exception.AppException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.*;

class AvatarStorageTest {
    @TempDir Path directory;
    final AvatarStorage storage = new AvatarStorage();

    @Test void usesVerifiedFormatAndUniqueSafeNames() throws Exception {
        byte[] png = new byte[]{(byte)137,80,78,71,13,10,26,10,0,0,0,0};
        var file = new MockMultipartFile("file", "../../attack.html", "image/png", png);
        String first = storage.store(9L, file, directory);
        String second = storage.store(9L, file, directory);
        assertThat(first).startsWith("/uploads/avatars/user_9_").endsWith(".png").isNotEqualTo(second);
        assertThat(Files.readAllBytes(directory.resolve(Path.of(first).getFileName()))).isEqualTo(png);
    }
    @Test void rejectsForgedContentAndOversizeFiles() {
        assertThatThrownBy(() -> storage.store(9L, new MockMultipartFile("file", "x.png", "image/png", "<script>bad</script>".getBytes()), directory)).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> storage.store(9L, new MockMultipartFile("file", "x.png", "image/png", new byte[5*1024*1024+1]), directory)).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> storage.store(9L, new MockMultipartFile("file", new byte[0]), directory)).isInstanceOf(AppException.class);
    }
}
