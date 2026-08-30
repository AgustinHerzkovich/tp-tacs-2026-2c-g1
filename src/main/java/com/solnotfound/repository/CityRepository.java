package com.solnotfound.repository;

import com.solnotfound.entity.City;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class CityRepository implements ICityRepository {
  private final Map<String, City> citiesByNormalizedName = new ConcurrentHashMap<>();

  /**
   * Finds a city by an accent-insensitive, case-insensitive normalized name or creates it
   * atomically. Blank names represent an absent city.
   *
   * @param name city display name
   * @return the canonical city, or {@code null} for a blank name
   */
  @Override
  public City findOrCreate(String name) {
    if (name == null || name.isBlank()) {
      return null;
    }

    String trimmedName = name.trim();
    return citiesByNormalizedName.computeIfAbsent(
        normalize(trimmedName), key -> new City(UUID.randomUUID().toString(), trimmedName));
  }

  private String normalize(String name) {
    return Normalizer.normalize(name, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT);
  }
}
