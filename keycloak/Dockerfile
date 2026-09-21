FROM quay.io/keycloak/keycloak:26.7.3

COPY keycloak-import/realm-export.json /opt/keycloak/data/import/realm-export.json
COPY keycloak-theme/planazo /opt/keycloak/themes/planazo

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--import-realm", "--http-enabled=true", "--proxy-headers=xforwarded", "--health-enabled=true"]
