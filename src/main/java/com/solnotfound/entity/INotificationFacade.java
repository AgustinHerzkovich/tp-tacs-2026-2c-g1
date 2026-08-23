package com.solnotfound.entity;

public interface INotificationFacade {
  public void notifyBadWeather(
      Activity activity,
      Weather
          weather); // basicamente, utiliza el factory para crear notificaciones a los usuarios de
  // dicha actividad
}
