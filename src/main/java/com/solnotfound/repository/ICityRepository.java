package com.solnotfound.repository;

import com.solnotfound.entity.City;

public interface ICityRepository {

  City findOrCreate(String name);
}
