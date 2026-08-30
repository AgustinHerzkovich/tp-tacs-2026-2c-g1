package com.solnotfound.repository;

import com.solnotfound.entity.Activity;
import java.util.List;

public interface IActivityRepository {

  void save(Activity activity);

  List<Activity> findAll();

  List<Activity> findActive();

  Activity findById(String id);

  void deleteAll();
}
