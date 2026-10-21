package com.solnotfound.entity.user;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "users")
public class User {
  @Id private String id;
  private String name;

  @Indexed(unique = true, sparse = true)
  private Long telegramChatId;

  /** Weather provider chosen by the user; {@code null} means the default one. */
  private WeatherProvider weatherProvider;

  public WeatherProvider effectiveWeatherProvider() {
    return weatherProvider == null ? WeatherProvider.OPEN_METEO : weatherProvider;
  }

  public static User withId(String id) {
    User user = new User();
    user.setId(id);
    return user;
  }
}
