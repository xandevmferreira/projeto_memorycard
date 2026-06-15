package com.memorycard.service;

import com.memorycard.config.GamingNewsProperties;
import com.memorycard.dto.response.GamingNewsItem;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class GamingNewsService {

    private static final Pattern HTML_TAGS = Pattern.compile("<[^>]+>");

    private final RestClient restClient;
    private final GamingNewsProperties properties;

    private List<GamingNewsItem> cachedNews = List.of();
    private Instant cacheExpiresAt = Instant.EPOCH;

    public GamingNewsService(RestClient restClient, GamingNewsProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public List<GamingNewsItem> getLatestNews() {
        if (!properties.isEnabled()) {
            return List.of();
        }

        if (Instant.now().isBefore(cacheExpiresAt) && !cachedNews.isEmpty()) {
            return cachedNews;
        }

        List<GamingNewsItem> items = new ArrayList<>();
        for (GamingNewsProperties.Feed feed : properties.getFeeds()) {
            items.addAll(fetchFeed(feed));
        }

        cachedNews = items.stream()
                .sorted(Comparator.comparing(GamingNewsItem::publishedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(properties.getMaxItems())
                .toList();

        cacheExpiresAt = Instant.now().plusSeconds(properties.getCacheMinutes() * 60L);
        return cachedNews;
    }

    private List<GamingNewsItem> fetchFeed(GamingNewsProperties.Feed feed) {
        try {
            String xml = restClient.get()
                    .uri(feed.getUrl())
                    .header("User-Agent", "MemoryCard/1.0")
                    .retrieve()
                    .body(String.class);

            if (xml == null || xml.isBlank()) {
                return List.of();
            }

            return parseRss(sanitizeXml(xml), feed.getName());
        } catch (Exception e) {
            return List.of();
        }
    }

    private String sanitizeXml(String xml) {
        return xml.replaceAll("(?is)<!DOCTYPE[^>]*>\\s*", "").trim();
    }

    private List<GamingNewsItem> parseRss(String xml, String source) {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);

            Document doc = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            NodeList itemNodes = doc.getElementsByTagName("item");
            List<GamingNewsItem> items = new ArrayList<>();

            for (int i = 0; i < itemNodes.getLength(); i++) {
                Element item = (Element) itemNodes.item(i);
                String title = textOf(item, "title");
                String link = textOf(item, "link");
                String description = textOf(item, "description");
                if (description == null || description.isBlank()) {
                    description = textOf(item, "content");
                }
                String pubDate = textOf(item, "pubDate");
                String imageUrl = extractImage(item, description);

                if (title == null || link == null) {
                    continue;
                }

                items.add(new GamingNewsItem(
                        title.trim(),
                        link.trim(),
                        stripHtml(description),
                        imageUrl,
                        source,
                        parseDate(pubDate)
                ));
            }
            return items;
        } catch (Exception e) {
            return List.of();
        }
    }

    private String extractImage(Element item, String description) {
        String media = attributeOf(item, "media:content", "url");
        if (media != null) {
            return media;
        }
        media = attributeOf(item, "media:thumbnail", "url");
        if (media != null) {
            return media;
        }
        if (description != null) {
            int srcIdx = description.indexOf("src=\"");
            if (srcIdx >= 0) {
                int start = srcIdx + 5;
                int end = description.indexOf('"', start);
                if (end > start) {
                    return description.substring(start, end);
                }
            }
        }
        return null;
    }

    private String textOf(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0) {
            return null;
        }
        Node node = nodes.item(0);
        return node.getTextContent();
    }

    private String attributeOf(Element parent, String tag, String attribute) {
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0) {
            return null;
        }
        Node node = nodes.item(0);
        if (node instanceof Element element) {
            return element.getAttribute(attribute);
        }
        return null;
    }

    private String stripHtml(String text) {
        if (text == null) {
            return null;
        }
        String cleaned = HTML_TAGS.matcher(text).replaceAll(" ").replaceAll("\\s+", " ").trim();
        return cleaned.length() > 180 ? cleaned.substring(0, 177) + "..." : cleaned;
    }

    private Instant parseDate(String pubDate) {
        if (pubDate == null || pubDate.isBlank()) {
            return null;
        }
        try {
            return ZonedDateTime.parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        } catch (Exception e) {
            try {
                return ZonedDateTime.parse(pubDate, DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH))
                        .toInstant();
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
