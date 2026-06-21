package lt.pauliusbaksys.datavault.service;
import lt.pauliusbaksys.datavault.dto.*;
import lt.pauliusbaksys.datavault.enums.AuditAction;
import lt.pauliusbaksys.datavault.enums.AuditLevel;
import lt.pauliusbaksys.datavault.enums.StorageType;
import lt.pauliusbaksys.datavault.model.File;
import lt.pauliusbaksys.datavault.model.User;
import lt.pauliusbaksys.datavault.repository.FileStorageRepository;
import lt.pauliusbaksys.datavault.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FileStorageService {

    private final UserRepository userRepository;
    private final FileStorageRepository fileStorageRepository;
    private final AuditLogService auditLogService;
    private final ObjectStorageResolver objectStorageResolver;
    private final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    public FileStorageService(UserRepository userRepository, FileStorageRepository fileStorageRepository, AuditLogService auditLogService, ObjectStorageResolver objectStorageResolver) {
        this.userRepository = userRepository;
        this.fileStorageRepository = fileStorageRepository;
        this.auditLogService = auditLogService;
        this.objectStorageResolver = objectStorageResolver;
    }

    public void storeFile(UUID userId, FileStorage meta, MultipartFile blob) throws IOException {
        boolean uploaded = false;
        String objectKey = null;

        if (meta.storageType() == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nepateiktas saugyklos tipas!");
        }

        StorageType storageType = meta.storageType();
        ObjectStorageService storageService = null;
        String storageName = storageType == StorageType.AWS_S3
                ? "AWS S3"
                : "Filebase IPFS";

        try {
            byte[] contentHash = Base64.getDecoder().decode(meta.contentHashB64());
            byte[] metaNonce = Base64.getDecoder().decode(meta.metaNonceB64());
            byte[] metaCipher = Base64.getDecoder().decode(meta.metaCipherB64());

            if (fileStorageRepository.existsByOwnerIdAndContentHash(userId, contentHash)){
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Toks failas jau yra!");
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Naudotojas nerastas: " + userId));

            objectKey = buildObjectKey(userId, meta.fileId());
            storageService = objectStorageResolver.resolve(storageType);

            storageService.upload(blob.getBytes(), objectKey);
            uploaded = true;

            File file = new File(
                    meta.fileId(),
                    user,
                    storageType,
                    objectKey,
                    meta.subKeyIdAsLong(),
                    contentHash,
                    metaNonce,
                    metaCipher
            );

            fileStorageRepository.save(file);

            auditLogService.log(
                    user,
                    AuditLevel.INFO,
                    AuditAction.FILE_UPLOAD,
                    "Užšifruotas failas sėkmingai išsaugotas " + storageName,
                    Map.of(
                            "fileId", file.getId(),
                            "objectKey", objectKey,
                            "storageType", storageType
                    )
            );
        }
        catch (IllegalArgumentException e) {
            if (uploaded) {
                deleteObjectQuietly(storageService, objectKey);
            }
            auditLogService.log(
                    userId,
                    AuditLevel.ALERT,
                    AuditAction.FILE_UPLOAD,
                    "Nepavyko įkelti užšifruoto failo: netinkamas Base64 formatas",
                    Map.of("reason", e.getClass().getSimpleName())
            );

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Užklausoje pateiktas netinkamas Base64 formatas",
                    e
            );
        }
        catch (DataIntegrityViolationException e) {
            if (uploaded) {
                deleteObjectQuietly(storageService, objectKey);
            }
            auditLogService.log(
                    userId,
                    AuditLevel.ALERT,
                    AuditAction.FILE_UPLOAD,
                    "Nepavyko įkelti užšifruoto failo: toks failas jau yra",
                    Map.of("reason", e.getClass().getSimpleName())
            );

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "DUPLICATE_FILE",
                    e
            );
        }
        catch (RuntimeException e) {
            if (uploaded) {
                deleteObjectQuietly(storageService, objectKey);
            }
            auditLogService.log(
                    userId,
                    AuditLevel.ALERT,
                    AuditAction.FILE_UPLOAD,
                    "Nepavyko įkelti užšifruoto failo",
                    Map.of("reason", e.getClass().getSimpleName())
            );
            throw e;
        }
    }

    private String buildObjectKey(UUID userId, UUID fileId){
        return "users/%s/files/%s.bin".formatted(userId, fileId);
    }

    // Calling it an object because this method doesn't handle file metadata
    private void deleteObjectQuietly(ObjectStorageService storageService, String objectKey) {
        if (storageService == null || objectKey == null) {
            return;
        }
        try {
            storageService.delete(objectKey);
        } catch (Exception ignored) {
        }
    }

    public void renameProvidedFile(UUID userId, UUID fileId, FileMetaCryptoPatch fileMetaPatch){
        try {
            File file = fileStorageRepository.findByIdAndOwner_Id(fileId, userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Naudotojo failo metaduomenys nerasti!"));

            file.setMetaNonce(Base64.getDecoder().decode(fileMetaPatch.metaNonceB64()));
            file.setMetaCipher(Base64.getDecoder().decode(fileMetaPatch.metaCipherB64()));

            fileStorageRepository.save(file);

            auditLogService.log(
                    userId,
                    AuditLevel.INFO,
                    AuditAction.FILE_RENAME,
                    "Užšifruoto failo metaduomenys sėkmingai pervadinti",
                    Map.of("fileId", file.getId())
            );
        }
        catch (IllegalArgumentException e){
            auditLogService.log(
                    userId,
                    AuditLevel.ALERT,
                    AuditAction.FILE_RENAME,
                    "Nepavyko pervadinti užšifruoto failo: netinkamas Base64 formatas",
                    Map.of(
                            "fileId", fileId,
                            "reason", e.getClass().getSimpleName()
                    )
            );
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Užklausoje pateiktas netinkamas Base64 formatas!", e);
        }
        catch (Exception e){
            auditLogService.log(
                    userId,
                    AuditLevel.ALERT,
                    AuditAction.FILE_RENAME,
                    "Nepavyko pervadinti užšifruoto failo",
                    Map.of(
                            "fileId", fileId,
                            "reason", e.getClass().getSimpleName()
                    )
            );
            throw e;
        }
    }

    public FileMetaSummaryPage getUserFilesMetasSummary(UUID userId, int page, int size){
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<File> filePage = fileStorageRepository.findAllByOwner_Id(userId, pageable);

        return new FileMetaSummaryPage(
                filePage.getContent().stream().map(file ->
                        new FileMetaSummary(file.getId(), file.getStorageType(), file.getCreatedAt())).toList(),
                filePage.getTotalElements()
        );
    }

    public FileMetaFull getUserFileMeta(UUID fileId, UUID userId){
        File file = fileStorageRepository.findByIdAndOwner_Id(fileId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Failo metaduomenys nerasti arba jie nepriklauso naudotojui!"));

        String subKeyIdStr = Long.toString(file.getSubKeyId());
        String metaNonceB64 = Base64.getEncoder().encodeToString(file.getMetaNonce());
        String metaCipherB64 = Base64.getEncoder().encodeToString(file.getMetaCipher());

        return new FileMetaFull(
                file.getId(),
                file.getStorageType(),
                file.getStorageRef(),
                subKeyIdStr,
                metaNonceB64,
                metaCipherB64
        );
    }

    public FileMetaCrypto getUserFileMetaCrypto(UUID fileId, UUID userId){
        File file = fileStorageRepository.findByIdAndOwner_Id(fileId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Failo metaduomenys nerasti arba jie nepriklauso naudotojui!"));

        return new FileMetaCrypto(
                Long.toString(file.getSubKeyId()),
                Base64.getEncoder().encodeToString(file.getMetaNonce()),
                Base64.getEncoder().encodeToString(file.getMetaCipher())
        );
    }

    public FileMetaFullPage getUserFilesMetas(UUID userId, int page, int size){
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<File> filePage = fileStorageRepository.findAllByOwner_Id(userId, pageable);

        return new FileMetaFullPage(
                filePage.getContent().stream().map((File ->
                        new FileMetaFull(
                                File.getId(),
                                File.getStorageType(),
                                File.getStorageRef(),
                                Long.toString(File.getSubKeyId()),
                                Base64.getEncoder().encodeToString(File.getMetaNonce()),
                                Base64.getEncoder().encodeToString(File.getMetaCipher())
                        ))).toList(),
                filePage.getTotalElements()
        );
    }

    public void deleteUserFile(UUID userId, UUID fileId){
        File file = fileStorageRepository.findByIdAndOwner_Id(fileId, userId)
                .orElseThrow(() -> {
                    auditLogService.log(
                            userId,
                            AuditLevel.ALERT,
                            AuditAction.FILE_DELETE,
                            "Nepavyko ištrinti failo: failas nerastas arba jis nepriklauso naudotojui",
                            Map.of("fileId", fileId,
                                    "reason", "USER_FILE_NOT_FOUND"
                            )
                    );
                   return new ResponseStatusException(HttpStatus.NOT_FOUND, "Failas nerastas arba jis nepriklauso naudotojui!");
                });

        StorageType storageType = file.getStorageType();
        String objectKey = file.getStorageRef();

        try{
            ObjectStorageService storageService = objectStorageResolver.resolve(storageType);

            storageService.delete(objectKey);

            fileStorageRepository.delete(file);

            auditLogService.log(
                    userId,
                    AuditLevel.INFO,
                    AuditAction.FILE_DELETE,
                    "Failas sėkmingai ištrintas",
                    Map.of("fileId", file.getId(),
                            "objectKey", objectKey,
                            "storageType", storageType
                    )
            );
        }
        // Because AWS SDK faults mostly are RuntimeException
        catch (RuntimeException e){
            auditLogService.log(
                    userId,
                    AuditLevel.ALERT,
                    AuditAction.FILE_DELETE,
                    "Nepavyko ištrinti failo",
                    Map.of("fileId", fileId,
                            "reason", e.getClass().getSimpleName()
                    )
            );
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Nepavyko ištrinti failo!", e);
        }
    }

    public List<FileMetaSummary> filterFileFilterItems(UUID userId, LocalDate fromDate, LocalDate toDate){
        LocalDate min = LocalDate.of(1970, 1, 1);

        OffsetDateTime from = (fromDate != null)
                ? fromDate.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime()
                : min.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();

        OffsetDateTime to = (toDate != null)
                ? toDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime() // exclusive
                : OffsetDateTime.now(ZoneOffset.UTC);

        return fileStorageRepository.findAllByOwner_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(userId, from, to)
                .stream()
                .map((f) -> new FileMetaSummary(
                        f.getId(),
                        f.getStorageType(),
                        f.getCreatedAt()))
                .toList();
    }

    public List<FileSubKey> getUserFilesSubkeyIds(UUID userId, List<UUID> fileIds){
        return fileStorageRepository.findAllByIdInAndOwner_Id(fileIds, userId).stream().map((f) ->
                new FileSubKey(
                        f.getId(),
                        String.valueOf(f.getSubKeyId())
        )).toList();
    }

    public ResponseEntity<byte[]> downloadEncryptedFile(UUID userId, UUID fileId){
        File file = fileStorageRepository.findByIdAndOwner_Id(fileId, userId)
                .orElseThrow(() -> {
                    auditLogService.log(
                            userId,
                            AuditLevel.ALERT,
                            AuditAction.FILE_DOWNLOAD,
                            "Nepavyko atsisiųsti failo: failas nerastas arba jis nepriklauso naudotojui",
                            Map.of("fileId", fileId,
                                    "reason", "USER_FILE_NOT_FOUND"
                            )
                    );
                   return new ResponseStatusException(HttpStatus.NOT_FOUND, "Failas nerastas!");
                });

        StorageType storageType = file.getStorageType();
        String objectKey = file.getStorageRef();

        try {

            ObjectStorageService storageService = objectStorageResolver.resolve(storageType);
            byte[] encryptedFile = storageService.download(objectKey);

            auditLogService.log(
                    userId,
                    AuditLevel.INFO,
                    AuditAction.FILE_DOWNLOAD,
                    "Failas sėkmingai atsisiųstas",
                    Map.of("fileId", fileId,
                            "objectKey", objectKey,
                            "storageType", storageType
                    )
            );

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(encryptedFile.length)
                    .body(encryptedFile);

        } catch (RuntimeException e) {
            auditLogService.log(
                    userId,
                    AuditLevel.ALERT,
                    AuditAction.FILE_DOWNLOAD,
                    "Nepavyko atsisiųsti failo",
                    Map.of(
                            "fileId", fileId,
                            "reason", e.getClass().getSimpleName()
                    )
            );

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Nepavyko atsisiųsti failo!",
                    e
            );
        }
    }
}
