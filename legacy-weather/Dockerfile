# Legacy weather provider (Humedad, Datos y Pronósticos S.A.). Only the compiled binary exists:
# it binds to 127.0.0.1:9977, so socat republishes it on 0.0.0.0:9978 for the other containers.
FROM debian:bookworm-slim

ARG TARGETARCH=amd64

RUN apt-get update \
 && apt-get install -y --no-install-recommends ca-certificates curl socat \
 && rm -rf /var/lib/apt/lists/*

COPY bin/bin/tacs-linux-${TARGETARCH} /usr/local/bin/tacs
COPY legacy-weather/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/tacs /usr/local/bin/entrypoint.sh

# The session (token.json) is written under $XDG_CONFIG_HOME/tacs and the API token is copied to
# /token/api-token; both are volumes so they survive restarts.
ENV XDG_CONFIG_HOME=/data
VOLUME ["/data", "/token"]

# /healthz and /readyz answer even while /rpc hangs, so they only prove the process is alive.
HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=3 \
  CMD curl -fsS --max-time 2 http://127.0.0.1:9977/healthz > /dev/null || exit 1

EXPOSE 9978
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
