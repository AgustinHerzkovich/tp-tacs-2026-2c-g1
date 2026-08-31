package com.solnotfound.repository;

import com.solnotfound.entity.activity.City;

public interface ICityRepository {

  City findOrCreate(String name);
}
