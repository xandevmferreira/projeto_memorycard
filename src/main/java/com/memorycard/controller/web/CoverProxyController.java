package com.memorycard.controller.web;

import com.memorycard.service.CoverImageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CoverProxyController {

    private final CoverImageService coverImageService;

    public CoverProxyController(CoverImageService coverImageService) {
        this.coverImageService = coverImageService;
    }

    @GetMapping("/covers/proxy")
    public ResponseEntity<byte[]> proxyCover(@RequestParam("url") String url) {
        return coverImageService.fetchImage(url);
    }
}
