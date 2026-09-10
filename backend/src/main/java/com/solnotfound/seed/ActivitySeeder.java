package com.solnotfound.seed;

import com.solnotfound.entity.activity.Activity;
import com.solnotfound.entity.activity.ActivityStatus;
import com.solnotfound.entity.activity.ActivityType;
import com.solnotfound.entity.activity.City;
import com.solnotfound.entity.activity.Location;
import com.solnotfound.entity.activity.ReprogramationRange;
import com.solnotfound.entity.user.User;
import com.solnotfound.entity.votation.Votation;
import com.solnotfound.entity.votation.VotationOption;
import com.solnotfound.entity.votation.VotationStatus;
import com.solnotfound.entity.weather.MaxRainProbabilityCondition;
import com.solnotfound.entity.weather.MaxWindCondition;
import com.solnotfound.entity.weather.TemperatureRangeCondition;
import com.solnotfound.repository.IActivityRepository;
import com.solnotfound.repository.IUserRepository;
import com.solnotfound.repository.IVotationRepository;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Seeds a handful of activities (plus their organizer/participant users, and one open votation) for
 * local development against a real MongoDB instance.
 *
 * <p>Disabled by default. Enable it for one run with {@code app.seed.enabled=true} (e.g. {@code
 * APP_SEED_ENABLED=true ./mvnw spring-boot:run}). Every seeded document uses a fixed,
 * human-readable id, so re-running this is idempotent: {@link IActivityRepository#save} / {@link
 * IUserRepository#save} / {@link IVotationRepository#save} upsert by id rather than duplicating
 * rows.
 *
 * <p>This intentionally goes through the real repositories/entities instead of inserting raw
 * documents, because several fields would be easy to get subtly wrong by hand: {@code
 * weatherConditions} is a polymorphic {@code List<WeatherCondition>} (Spring Data writes a {@code
 * _class} discriminator per element), and {@code organizer}/{@code participants}/{@code
 * VotationOption.users} are {@code @DocumentReference} fields resolved against the {@code users}
 * collection.
 */
@Component
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class ActivitySeeder implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(ActivitySeeder.class);

  private final IActivityRepository activityRepository;
  private final IUserRepository userRepository;
  private final IVotationRepository votationRepository;

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "Spring injects the shared repository beans")
  public ActivitySeeder(
      IActivityRepository activityRepository,
      IUserRepository userRepository,
      IVotationRepository votationRepository) {
    this.activityRepository = activityRepository;
    this.userRepository = userRepository;
    this.votationRepository = votationRepository;
  }

  @Override
  public void run(String... args) {
    log.info("Seeding activities (app.seed.enabled=true)...");

    Map<String, User> users = seedUsers();
    LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);

    seedAsado(users, now);
    seedTrekkingWithVotation(users, now);
    seedCumpleanios(users, now);
    seedPicnic(users, now);
    seedVoley(users, now);
    seedJuegos(users, now);

    log.info("Seed complete: {} users, 6 activities.", users.size());
  }

  /**
   * Same cast of mock users the frontend's login screen offers (src/data/mockUsers.ts) — logging in
   * as one of them lines up with real organizer/participant data once the frontend switches from
   * mocked activities to these seeded ones.
   */
  private Map<String, User> seedUsers() {
    Map<String, String> names =
        Map.of(
            "development-user", "Juan Luengo",
            "vale-rios", "Vale Ríos",
            "fede-molina", "Fede Molina",
            "cami-paz", "Cami Paz",
            "nico-gimenez", "Nico Giménez",
            "sol-alvarez", "Sol Álvarez");

    return names.entrySet().stream()
        .collect(
            java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                  User user = new User();
                  user.setId(entry.getKey());
                  user.setName(entry.getValue());
                  return userRepository.save(user);
                }));
  }

  private void seedAsado(Map<String, User> users, LocalDateTime now) {
    Activity activity = new Activity();
    activity.setId("asado");
    activity.setTitle("Asado en Parque Centenario");
    activity.setDescription("Junta con amigos, llevar sillas y algo para compartir.");
    activity.setType(ActivityType.OUTDOOR);
    activity.setLocation(new Location(new City(null, "Buenos Aires"), -34.6037, -58.3816));
    activity.setDateTime(now.plusDays(7).withHour(14).withMinute(0));
    activity.setMinParticipants(4);
    activity.setMaxParticipants(15);
    activity.setOrganizer(users.get("development-user"));
    activity.setParticipants(
        List.of(users.get("vale-rios"), users.get("fede-molina"), users.get("cami-paz")));
    activity.setWeatherConditions(
        List.of(
            new MaxRainProbabilityCondition(60),
            new TemperatureRangeCondition(10, 34),
            new MaxWindCondition(35.0)));
    activity.setAnticipationWindow(24);
    activity.setReprogramationRange(
        new ReprogramationRange(3, LocalTime.of(10, 0), LocalTime.of(20, 0)));
    // Status starts CONFIRMED by default — nothing else to set.
    activityRepository.save(activity);
  }

  private void seedTrekkingWithVotation(Map<String, User> users, LocalDateTime now) {
    Activity activity = new Activity();
    activity.setId("trekking");
    activity.setTitle("Trekking Cerro Otto");
    activity.setDescription(
        "Subida moderada con vistas al lago, ida y vuelta en el día. Llevar calzado de trekking,"
            + " agua y protector solar.");
    activity.setType(ActivityType.OUTDOOR);
    activity.setLocation(new Location(new City(null, "Bariloche"), -41.1335, -71.3103));
    LocalDateTime originalDateTime = now.plusDays(5).withHour(9).withMinute(0);
    activity.setDateTime(originalDateTime);
    activity.setMinParticipants(4);
    activity.setMaxParticipants(12);
    activity.setOrganizer(users.get("vale-rios"));
    activity.setParticipants(
        List.of(
            users.get("development-user"),
            users.get("fede-molina"),
            users.get("cami-paz"),
            users.get("nico-gimenez")));
    activity.setWeatherConditions(
        List.of(
            new MaxRainProbabilityCondition(40),
            new TemperatureRangeCondition(5, 25),
            new MaxWindCondition(25.0)));
    activity.setAnticipationWindow(24);
    activity.setReprogramationRange(
        new ReprogramationRange(3, LocalTime.of(8, 0), LocalTime.of(18, 0)));
    activity.setStatus(ActivityStatus.PROPOSED);
    activityRepository.save(activity);

    Votation votation = new Votation();
    votation.setId("trekking-votation");
    votation.setActivity(activity);
    votation.setCreationDate(now);
    votation.setClosingDate(now.plusDays(2));
    votation.setStatus(VotationStatus.ACTIVE);

    VotationOption optionA = new VotationOption();
    optionA.setDateTime(originalDateTime.plusDays(1));
    optionA.setUsers(
        List.of(
            users.get("development-user"), users.get("fede-molina"), users.get("nico-gimenez")));

    VotationOption optionB = new VotationOption();
    optionB.setDateTime(originalDateTime.plusDays(1).withHour(15));
    optionB.setUsers(List.of(users.get("vale-rios"), users.get("cami-paz")));

    VotationOption optionC = new VotationOption();
    optionC.setDateTime(originalDateTime.plusDays(2));
    optionC.setUsers(List.of());

    votation.setOptions(List.of(optionA, optionB, optionC));
    votationRepository.save(votation);
  }

  private void seedCumpleanios(Map<String, User> users, LocalDateTime now) {
    Activity activity = new Activity();
    activity.setId("cumple");
    activity.setTitle("Cumpleaños de Vale");
    activity.setDescription("Cumple sorpresa — el lugar exacto se confirma más cerca de la fecha.");
    activity.setType(ActivityType.INDOOR);
    activity.setLocation(
        new Location(new City(null, "San Telmo, Buenos Aires"), -34.6212, -58.3731));
    activity.setDateTime(now.plusDays(6).withHour(21).withMinute(0));
    activity.setMinParticipants(8);
    activity.setMaxParticipants(20);
    activity.setOrganizer(users.get("development-user"));
    activity.setParticipants(
        List.of(
            users.get("vale-rios"),
            users.get("fede-molina"),
            users.get("cami-paz"),
            users.get("nico-gimenez"),
            users.get("sol-alvarez")));
    activity.setWeatherConditions(
        List.of(
            new MaxRainProbabilityCondition(80),
            new TemperatureRangeCondition(-5, 40),
            new MaxWindCondition(50.0)));
    activity.setAnticipationWindow(12);
    activity.setReprogramationRange(
        new ReprogramationRange(7, LocalTime.of(0, 0), LocalTime.of(23, 59)));
    activity.setStatus(ActivityStatus.RESCHEDULED);
    activityRepository.save(activity);
  }

  private void seedPicnic(Map<String, User> users, LocalDateTime now) {
    Activity activity = new Activity();
    activity.setId("picnic");
    activity.setTitle("Picnic Costanera");
    activity.setDescription("Cancelada por lluvia — pendiente re-organizar para otra fecha.");
    activity.setType(ActivityType.OUTDOOR);
    activity.setLocation(new Location(new City(null, "Buenos Aires"), -34.5915, -58.3626));
    activity.setDateTime(now.minusDays(10).withHour(17).withMinute(0));
    activity.setMinParticipants(6);
    activity.setMaxParticipants(10);
    activity.setOrganizer(users.get("fede-molina"));
    activity.setParticipants(List.of(users.get("development-user"), users.get("vale-rios")));
    activity.setWeatherConditions(
        List.of(
            new MaxRainProbabilityCondition(30),
            new TemperatureRangeCondition(15, 30),
            new MaxWindCondition(20.0)));
    activity.setAnticipationWindow(24);
    activity.setReprogramationRange(
        new ReprogramationRange(3, LocalTime.of(10, 0), LocalTime.of(20, 0)));
    activity.setStatus(ActivityStatus.CANCELLED);
    activityRepository.save(activity);
  }

  private void seedVoley(Map<String, User> users, LocalDateTime now) {
    Activity activity = new Activity();
    activity.setId("voley");
    activity.setTitle("Torneo de vóley playero");
    activity.setDescription("Torneo amistoso en la playa, se arma por equipos en el momento.");
    activity.setType(ActivityType.OUTDOOR);
    activity.setLocation(new Location(new City(null, "Mar del Plata"), -38.0055, -57.5426));
    activity.setDateTime(now.plusDays(9).withHour(10).withMinute(0));
    activity.setMinParticipants(8);
    activity.setMaxParticipants(20);
    activity.setOrganizer(users.get("cami-paz"));
    activity.setParticipants(List.of(users.get("nico-gimenez"), users.get("sol-alvarez")));
    activity.setWeatherConditions(
        List.of(
            new MaxRainProbabilityCondition(50),
            new TemperatureRangeCondition(15, 35),
            new MaxWindCondition(30.0)));
    activity.setAnticipationWindow(24);
    activity.setReprogramationRange(
        new ReprogramationRange(3, LocalTime.of(9, 0), LocalTime.of(19, 0)));
    activityRepository.save(activity);
  }

  private void seedJuegos(Map<String, User> users, LocalDateTime now) {
    Activity activity = new Activity();
    activity.setId("juegos");
    activity.setTitle("Noche de juegos de mesa");
    activity.setDescription("Traé tu juego favorito, hay para todos los gustos.");
    activity.setType(ActivityType.INDOOR);
    activity.setLocation(new Location(new City(null, "Palermo, Buenos Aires"), -34.5895, -58.4306));
    activity.setDateTime(now.plusDays(3).withHour(20).withMinute(0));
    activity.setMinParticipants(4);
    activity.setMaxParticipants(8);
    activity.setOrganizer(users.get("fede-molina"));
    activity.setParticipants(List.of(users.get("development-user"), users.get("vale-rios")));
    activity.setWeatherConditions(
        List.of(
            new MaxRainProbabilityCondition(90),
            new TemperatureRangeCondition(-10, 45),
            new MaxWindCondition(60.0)));
    activity.setAnticipationWindow(6);
    activity.setReprogramationRange(
        new ReprogramationRange(2, LocalTime.of(18, 0), LocalTime.of(23, 0)));
    activityRepository.save(activity);
  }
}
