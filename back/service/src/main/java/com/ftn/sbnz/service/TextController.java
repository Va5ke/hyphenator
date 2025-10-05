package com.ftn.sbnz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ftn.sbnz.model.models.EventRequest;


@RestController
@RequestMapping("/api/text")
public class TextController {

    @Autowired
    private TextService textService;

    @PostMapping("/event")
    public ResponseEntity<String> handleEvent(@RequestBody EventRequest request) {
        String updatedText = textService.processEvent(request.getNumber());
        return ResponseEntity.ok(updatedText);
    }

    @PostMapping("/reset")
    public ResponseEntity<String> resetText() {
        textService.resetText();
        return ResponseEntity.ok("Text reset successfully");
    }
}