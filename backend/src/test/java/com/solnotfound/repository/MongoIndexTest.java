package com.solnotfound.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.solnotfound.entity.activity.Activity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

/**
 * Checks that the indexes declared on the entities are created from application.properties at
 * startup. Kept apart from {@link MongoPersistenceTest} because that class drops the database
 * before each test, which also drops the indexes.
 */
@DataMongoTest
@Testcontainers
class MongoIndexTest {

  @Container @ServiceConnection
  static final MongoDBContainer MONGODB = new MongoDBContainer("mongo:8.0.14");

  @Autowired private MongoTemplate mongoTemplate;

  @Test
  void createsActivityIndexesForPagedListingsOnStartup() {
    List<String> indexNames =
        mongoTemplate.indexOps(Activity.class).getIndexInfo().stream()
            .map(IndexInfo::getName)
            .toList();

    assertThat(indexNames)
        .contains("organizer_date_time", "participants_date_time", "dateTime", "status");
  }

  @Test
  void pagedListingsUseAnIndexInsteadOfScanningAndSortingInMemory() {
    assertThat(planStages(new Document(), new Document("dateTime", 1)))
        .contains("IXSCAN")
        .doesNotContain("COLLSCAN", "SORT");
    assertThat(planStages(new Document("organizer", "organizer"), new Document("dateTime", 1)))
        .contains("IXSCAN")
        .doesNotContain("COLLSCAN", "SORT");
    assertThat(planStages(new Document("participants", "participant"), new Document("dateTime", 1)))
        .contains("IXSCAN")
        .doesNotContain("COLLSCAN", "SORT");
  }

  private List<String> planStages(Document filter, Document sort) {
    Document explain =
        mongoTemplate
            .getCollection(mongoTemplate.getCollectionName(Activity.class))
            .find(filter)
            .sort(sort)
            .limit(12)
            .explain();
    List<String> stages = new ArrayList<>();
    collectStages(explain.get("queryPlanner", Document.class).get("winningPlan"), stages);
    return stages;
  }

  private void collectStages(Object node, List<String> stages) {
    if (node instanceof Map<?, ?> map) {
      if (map.get("stage") instanceof String stage) {
        stages.add(stage);
      }
      map.values().forEach(value -> collectStages(value, stages));
    } else if (node instanceof List<?> list) {
      list.forEach(value -> collectStages(value, stages));
    }
  }
}
