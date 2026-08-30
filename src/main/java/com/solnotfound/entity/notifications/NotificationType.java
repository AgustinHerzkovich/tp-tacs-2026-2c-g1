package com.solnotfound.entity.notifications;

import com.solnotfound.entity.Activity;

public interface NotificationType {

  String code();

  String generateTitle(Activity activity);

  String generateMessage(Activity activity);
}
