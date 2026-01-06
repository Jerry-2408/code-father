package com.example.codefather.service;

import org.springframework.stereotype.Service;

public interface ScreenshotService {

    String generateAndUploadScreenshot(String webUrl);
}
