package com.theinfectedhour.backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.theinfectedhour.backend.service.SaveService;

/** REST endpoints for uploading and downloading save games. */
@RestController
@RequestMapping("/api/saves")
public class SaveController {

    private final SaveService saveService;

    public SaveController(SaveService saveService) {
        this.saveService = saveService;
    }

    // TODO: implement endpoints
}
