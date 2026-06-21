package lt.pauliusbaksys.datavault.service;

import lt.pauliusbaksys.datavault.enums.StorageType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This {@link ObjectStorageResolver}s purpose is to resolve the correct object storage service by selecting storage type.
 *
 * <p> We don't need to check the storage type with multiple if/else statements.
 * Each storage provider has its own service implementation (AWS S3 or Filebase),
 * and this resolver returns the correct one when it is needed.</p>
 *
 * <p>Spring automatically injects all beans that implement ObjectStorageService
 * into the constructor.<br>
 * So if AwsS3StorageService and
 * FilebaseStorageService are marked with @Service, both of them will be added
 * to the storageServices list automatically.</p>
 */
@Service
public class ObjectStorageResolver {

    private final Map<StorageType, ObjectStorageService> services = new HashMap<>();

    public ObjectStorageResolver(List<ObjectStorageService> storageServices) {
        for (ObjectStorageService service : storageServices){
            services.put(service.getStorageType(), service);
        }
    }

    public ObjectStorageService resolve(StorageType storageType){
        ObjectStorageService service = services.get(storageType);

        if (service == null){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Nepalaikomas saugyklos tipas: " + storageType
            );
        }

        return service;
    }
}
