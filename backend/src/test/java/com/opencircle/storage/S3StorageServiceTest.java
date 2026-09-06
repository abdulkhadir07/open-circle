package com.opencircle.storage;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class S3StorageServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-06T12:00:00Z");

    private final S3Client s3Client = mock(S3Client.class);
    private final S3Presigner s3Presigner = mock(S3Presigner.class);
    private final StorageProperties properties = storageProperties();

    private final S3StorageService service = new S3StorageService(
            s3Client,
            s3Presigner,
            properties,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void uploadStoresObjectInConfiguredBucket() {
        byte[] bytes = "hello".getBytes(UTF_8);

        StoredFile storedFile = service.upload(
                "chat-attachments/room-123",
                new ByteArrayInputStream(bytes),
                bytes.length,
                " image/png "
        );

        var requestCaptor = forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest request = requestCaptor.getValue();

        assertThat(request.bucket()).isEqualTo("opencircle-test-attachments");
        assertThat(request.key()).startsWith("chat-attachments/room-123/");
        assertThat(request.contentType()).isEqualTo("image/png");
        assertThat(request.contentLength()).isEqualTo((long) bytes.length);

        assertThat(storedFile.bucket()).isEqualTo("opencircle-test-attachments");
        assertThat(storedFile.key()).isEqualTo(request.key());
        assertThat(storedFile.contentType()).isEqualTo("image/png");
        assertThat(storedFile.fileSizeBytes()).isEqualTo(bytes.length);
    }

    @Test
    void uploadRejectsMissingFileContent() {
        assertThatThrownBy(() -> service.upload("chat-attachments/room-123", null, 10L, "image/png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File content is required");

        verifyNoInteractions(s3Client);
    }

    @Test
    void uploadRejectsInvalidFileSize() {
        assertThatThrownBy(() -> service.upload(
                "chat-attachments/room-123",
                new ByteArrayInputStream(new byte[0]),
                0L,
                "image/png"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File size must be greater than zero");

        verifyNoInteractions(s3Client);
    }

    @Test
    void uploadRejectsInvalidKeyPrefix() {
        assertThatThrownBy(() -> service.upload(
                "../private",
                new ByteArrayInputStream("hello".getBytes(UTF_8)),
                5L,
                "image/png"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Storage key prefix is invalid");

        verifyNoInteractions(s3Client);
    }

    @Test
    void uploadWrapsS3Failure() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkClientException.builder().message("S3 unavailable").build());

        assertThatThrownBy(() -> service.upload(
                "chat-attachments/room-123",
                new ByteArrayInputStream("hello".getBytes(UTF_8)),
                5L,
                "image/png"
        ))
                .isInstanceOf(StorageException.class)
                .hasMessage("Unable to store uploaded file");
    }

    @Test
    void generateDownloadUrlUsesConfiguredExpirationAndSafeFilename() throws Exception {
        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(URI.create("https://example.com/download").toURL());
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);

        AttachmentDownloadUrl downloadUrl = service.generateDownloadUrl(
                "opencircle-test-attachments",
                "chat-attachments/file-id",
                "../bad\"file\nname.png"
        );

        var requestCaptor = forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(requestCaptor.capture());

        GetObjectPresignRequest presignRequest = requestCaptor.getValue();
        GetObjectRequest objectRequest = presignRequest.getObjectRequest();

        assertThat(downloadUrl.url()).isEqualTo(URI.create("https://example.com/download"));
        assertThat(downloadUrl.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(10)));
        assertThat(presignRequest.signatureDuration()).isEqualTo(Duration.ofMinutes(10));
        assertThat(objectRequest.bucket()).isEqualTo("opencircle-test-attachments");
        assertThat(objectRequest.key()).isEqualTo("chat-attachments/file-id");
        assertThat(objectRequest.responseContentDisposition()).isEqualTo("attachment; filename=\"bad_file_name.png\"");
    }

    @Test
    void generateDownloadUrlWrapsS3Failure() {
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenThrow(SdkClientException.builder().message("S3 unavailable").build());

        assertThatThrownBy(() -> service.generateDownloadUrl(
                "opencircle-test-attachments",
                "chat-attachments/file-id",
                "flyer.png"
        ))
                .isInstanceOf(StorageException.class)
                .hasMessage("Unable to create download URL");
    }

    private StorageProperties storageProperties() {
        StorageProperties properties = new StorageProperties();
        properties.getS3().setBucket("opencircle-test-attachments");
        properties.getS3().setRegion("us-east-1");
        properties.getS3().setPresignedUrlExpirationMinutes(10);
        properties.getAttachments().setMaxFileSizeBytes(10_485_760L);
        properties.getAttachments().setAllowedContentTypes(
                java.util.List.of("image/jpeg", "image/png", "image/webp", "application/pdf")
        );

        return properties;
    }
}