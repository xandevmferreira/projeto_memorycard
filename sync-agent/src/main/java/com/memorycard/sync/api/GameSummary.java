package com.memorycard.sync.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GameSummary(long id, String title) {
    @Override
    public String toString() {
        return title + " (#" + id + ")";
    }
}
