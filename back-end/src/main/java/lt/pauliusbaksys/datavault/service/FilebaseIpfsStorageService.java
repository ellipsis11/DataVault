package lt.pauliusbaksys.datavault.service;

import lt.pauliusbaksys.datavault.config.FilebaseIpfsProperties;
import lt.pauliusbaksys.datavault.enums.StorageType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class FilebaseIpfsStorageService implements ObjectStorageService{

    private final S3Client s3Client;
    private final FilebaseIpfsProperties properties;

    public FilebaseIpfsStorageService(
            @Qualifier("filebaseS3Client") S3Client s3Client,
            FilebaseIpfsProperties properties
    ) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.FILEBASE_IPFS;
    }

    @Override
    public void upload(byte[] bytes, String objectKey) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .contentType("application/octet-stream")
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(bytes));
    }

    @Override
    public byte[] download(String objectKey) {
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
