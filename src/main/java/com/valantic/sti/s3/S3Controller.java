package com.valantic.sti.s3;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/s3")
public class S3Controller {

    private final S3Service s3Service;

    public S3Controller(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @PostMapping("/upload")
    public void uploadFile(@RequestParam String bucket, 
                          @RequestParam String key, 
                          @RequestParam MultipartFile file) throws Exception {
        s3Service.uploadFile(bucket, key, file.getBytes());
    }

    @GetMapping("/download")
    public byte[] downloadFile(@RequestParam String bucket, 
                              @RequestParam String key) throws IOException {
        return s3Service.downloadFile(bucket, key);
    }
}
