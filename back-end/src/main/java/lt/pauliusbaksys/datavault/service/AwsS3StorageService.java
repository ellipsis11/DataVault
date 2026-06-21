package lt.pauliusbaksys.datavault.service;

import lt.pauliusbaksys.datavault.config.AwsS3Properties;
import lt.pauliusbaksys.datavault.enums.StorageType;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class AwsS3StorageService implements ObjectStorageService{
    private final S3Client s3Client;
    private final AwsS3Properties properties;

    public AwsS3StorageService(S3Client s3Client, AwsS3Properties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    @Override
    public StorageType getStorageType(){
        return StorageType.AWS_S3;
    }

    /**
     * For uploading an encrypted file object to the configured S3 bucket.
     *
     * @param bytes encrypted file content to upload.
     * @param objectKey path/name of the object inside the bucket (users/{userId}/files/{fileId}.bin)
     * @param contentType MIME type of the uploaded object (application/octet-stream) (byte stream)
     */
    @Override
    public void upload(byte[] bytes, String objectKey){
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .contentType("application/octet-stream")
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(bytes));
    }

    @Override
    public byte[] download(String objectKey){
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .build();

        return s3Client.getObjectAsBytes(request).asByteArray();
    }

    @Override
    public void delete(String objectKey){
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .build();

        s3Client.deleteObject(request);
    }
}
