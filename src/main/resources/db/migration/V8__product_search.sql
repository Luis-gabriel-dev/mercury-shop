-- V8: busca full-text no catálogo (Fase 9).
-- Coluna tsvector GERADA a partir de nome+descrição (config 'portuguese' → stemming/plural) e
-- índice GIN para busca rápida com ranking. A coluna não é mapeada na entidade JPA (ddl-auto:
-- validate ignora colunas extras); as buscas usam query nativa com plainto_tsquery/ts_rank.
alter table products
    add column search_vector tsvector
    generated always as (
        to_tsvector('portuguese', coalesce(name, '') || ' ' || coalesce(description, ''))
    ) stored;

create index idx_products_search on products using gin (search_vector);
