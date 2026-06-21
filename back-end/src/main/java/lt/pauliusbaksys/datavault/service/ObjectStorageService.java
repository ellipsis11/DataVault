package lt.pauliusbaksys.datavault.service;

import lt.pauliusbaksys.datavault.enums.StorageType;

public interface ObjectStorageService {

    StorageType getStorageType();

    void upload(byte[] bytes, String objectKey);

    byte[] download(String objectKey);

    void delete(String objectKey);
}
