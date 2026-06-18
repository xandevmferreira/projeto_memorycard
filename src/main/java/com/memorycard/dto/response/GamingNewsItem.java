package com.memorycard.dto.response;

import java.time.Instant;

public record GamingNewsItem(
        String title,
        String url,
        String summary,
        String imageUrl,
        String source,
        Instant publishedAt
) {}
