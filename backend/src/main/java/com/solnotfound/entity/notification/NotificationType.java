package com.solnotfound.entity.notification;

import com.solnotfound.entity.activity.Activity;

public interface NotificationType {

  String code();

  String generateTitle(Activity activity);

  String generateMessage(Activity activity);
}
