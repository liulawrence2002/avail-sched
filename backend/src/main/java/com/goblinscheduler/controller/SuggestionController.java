package com.goblinscheduler.controller;

import com.goblinscheduler.dto.SlotSuggestionDto;
import com.goblinscheduler.service.SuggestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SuggestionController {
    private final SuggestionService suggestionService;

    public SuggestionController(SuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @GetMapping("/api/host/{hostToken}/suggestions")
    public ResponseEntity<List<SlotSuggestionDto>> getSuggestions(@PathVariable String hostToken) {
        return ResponseEntity.ok(suggestionService.getSuggestions(hostToken));
    }
}
