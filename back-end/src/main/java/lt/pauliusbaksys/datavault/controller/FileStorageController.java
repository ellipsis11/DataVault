package lt.pauliusbaksys.datavault.controller;
import jakarta.servlet.http.HttpServletRequest;
import lt.pauliusbaksys.datavault.dto.*;
import lt.pauliusbaksys.datavault.service.CurrentUserService;
import lt.pauliusbaksys.datavault.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/file-storage")
public class FileStorageController {

    private final FileStorageService fileStorageService;
    private final CurrentUserService currentUserService;
    private final Logger logger = LoggerFactory.getLogger(FileStorageController.class);

    public FileStorageController(FileStorageService fileStorageService, CurrentUserService currentUserService) {
        this.fileStorageService = fileStorageService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public Map<String, String> getFileStoragePage(){
        return Map.of("message", "access-granted");
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadEncryptedFile(
            HttpServletRequest req,
            @RequestPart("meta")FileStorage meta,
            @RequestPart("blob")MultipartFile blob){
        try{
            fileStorageService.storeFile(currentUserService.getCurrentUserId(req), meta, blob);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }
        catch (IOException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Nepavyko išsaugoti failo!");
        }
    }

    /** Using patch because we are doing partial update, not full replacement (whole file entity) */
    @PatchMapping("/{fileId}/rename")
    public ResponseEntity<?> renameFile(HttpServletRequest req, @PathVariable UUID fileId, @RequestBody FileMetaCryptoPatch fileMetaPatch){
        fileStorageService.renameProvidedFile(currentUserService.getCurrentUserId(req), fileId, fileMetaPatch);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<byte[]> donwloadEncryptedFile(HttpServletRequest req, @PathVariable UUID fileId){
        return fileStorageService.downloadEncryptedFile(currentUserService.getCurrentUserId(req), fileId);
    }

    @PostMapping("/subKey-ids")
    public List<FileSubKey> getUserSubKeyIds(HttpServletRequest req, @RequestBody List<UUID> fileIds ){
        return fileStorageService.getUserFilesSubkeyIds(currentUserService.getCurrentUserId(req), fileIds);
    }

    @GetMapping("/meta")
    public FileMetaFullPage getUserFileMetasFull(HttpServletRequest req,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size){
        return fileStorageService.getUserFilesMetas(currentUserService.getCurrentUserId(req), page, size);
    }

    @GetMapping("/{fileId}/meta")
    public FileMetaFull getUserFileMetaFull(HttpServletRequest req, @PathVariable UUID fileId){
        return fileStorageService.getUserFileMeta(fileId, currentUserService.getCurrentUserId(req));
    }

    @GetMapping("/{fileId}/meta-crypto")
    public FileMetaCrypto getUserFileMetaCrypto(HttpServletRequest req, @PathVariable UUID fileId){
        return fileStorageService.getUserFileMetaCrypto(fileId, currentUserService.getCurrentUserId(req));
    }

    @GetMapping("/meta-sum")
    public FileMetaSummaryPage getUserFileMetasSummary(HttpServletRequest req,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "10") int size){
        return fileStorageService.getUserFilesMetasSummary(currentUserService.getCurrentUserId(req), page, size);
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<?> deleteFile(HttpServletRequest req, @PathVariable UUID fileId){
        fileStorageService.deleteUserFile(currentUserService.getCurrentUserId(req), fileId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/filter")
    public List<FileMetaSummary> filterFiles(HttpServletRequest req, @RequestParam(required = false) LocalDate from,
                                            @RequestParam(required = false) LocalDate to){
        return fileStorageService.filterFileFilterItems(currentUserService.getCurrentUserId(req), from, to);
    }
}
