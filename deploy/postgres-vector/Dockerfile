FROM postgres:16-bookworm@sha256:64154d0babcb1741988719e703419af0382b19953706149f9872fbd0f438efa8

ARG PGVECTOR_VERSION=0.8.2

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        build-essential \
        ca-certificates \
        curl \
        postgresql-server-dev-16 \
    && curl -fsSL "https://github.com/pgvector/pgvector/archive/refs/tags/v${PGVECTOR_VERSION}.tar.gz" -o /tmp/pgvector.tar.gz \
    && tar -xzf /tmp/pgvector.tar.gz -C /tmp \
    && make -C "/tmp/pgvector-${PGVECTOR_VERSION}" \
    && make -C "/tmp/pgvector-${PGVECTOR_VERSION}" install \
    && rm -rf /tmp/pgvector* \
    && apt-get purge -y --auto-remove build-essential curl postgresql-server-dev-16 \
    && rm -rf /var/lib/apt/lists/*
