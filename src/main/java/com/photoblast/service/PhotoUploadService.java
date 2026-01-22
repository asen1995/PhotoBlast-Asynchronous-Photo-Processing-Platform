package com.photoblast.service;

import com.photoblast.dto.PhotoUploadResponse;
import com.photoblast.model.PhotoProcessingJob.ProcessingTask;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PhotoUploadService {

    PhotoUploadResponse uploadPhoto(MultipartFile file, List<ProcessingTask> tasks);
}
