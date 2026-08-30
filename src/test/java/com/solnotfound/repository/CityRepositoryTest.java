package com.solnotfound.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.solnotfound.entity.City;
import org.junit.jupiter.api.Test;

class CityRepositoryTest {

  @Test
  void reusesCityByNormalizedName() {
    CityRepository repository = new CityRepository();

    City first = repository.findOrCreate("Córdoba");
    City second = repository.findOrCreate(" cordoba ");

    assertThat(second).isSameAs(first);
  }
}
