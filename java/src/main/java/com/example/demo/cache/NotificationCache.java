package com.example.demo.cache;

import java.util.List;

public interface NotificationCache {
    // For single object caching
    void put(String key, Object value);
    Object get(String key);
    void delete(String key);

    // For the "Recent Notifications" list
    void pushToRecent(Object value);
    List<Object> getRecent(int limit);
    void clearRecent();
    void pushToRecentAtomic(Object value);
}