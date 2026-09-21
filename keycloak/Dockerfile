FROM maven:3.9.11-eclipse-temurin-21 AS provider

ARG CLOUD_SQL_SOCKET_FACTORY_VERSION=1.28.2

COPY keycloak/cloud-sql-pom.xml /build/pom.xml

RUN mvn -f /build/pom.xml dependency:copy-dependencies \
    -Dcloud-sql-socket-factory.version=${CLOUD_SQL_SOCKET_FACTORY_VERSION} \
    -DoutputDirectory=/providers

FROM quay.io/keycloak/keycloak:26.7.3 AS builder

COPY keycloak-import/realm-export.json /opt/keycloak/data/import/realm-export.json
COPY keycloak-theme/planazo /opt/keycloak/themes/planazo
COPY --from=provider --chown=keycloak:keycloak /providers/ /opt/keycloak/providers/

RUN /opt/keycloak/bin/kc.sh build --db=postgres --health-enabled=true

FROM quay.io/keycloak/keycloak:26.7.3

COPY --from=builder /opt/keycloak/ /opt/keycloak/

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--optimized", "--import-realm", "--http-enabled=true", "--proxy-headers=xforwarded"]
