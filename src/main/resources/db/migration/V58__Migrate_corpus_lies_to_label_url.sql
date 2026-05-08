-- Migration corpus_lies vers format "label|url; label|url"
-- - Conserve les entrées déjà au format label|url
-- - Convertit "label:https://..." -> "label|https://..."
-- - Convertit "https://..." -> "https://...|https://..."
-- - Laisse inchangées les entrées non convertibles (ex. libellé seul)

WITH tokens AS (
    SELECT
        em.entity_id,
        trim(tok) AS tok
    FROM entity_metadata em
    CROSS JOIN LATERAL regexp_split_to_table(COALESCE(em.corpus_lies, ''), '[;；]') AS tok
    WHERE em.corpus_lies IS NOT NULL
      AND btrim(em.corpus_lies) <> ''
      AND btrim(tok) <> ''
),
normalized AS (
    SELECT
        entity_id,
        CASE
            WHEN tok LIKE '%|%' THEN tok
            WHEN tok ~* '^(.*):(https?://.*)$' THEN regexp_replace(tok, '^(.*):(https?://.*)$', '\1|\2', 'i')
            WHEN tok ~* '^https?://' THEN tok || '|' || tok
            ELSE tok
        END AS normalized_tok
    FROM tokens
),
rejoined AS (
    SELECT
        entity_id,
        string_agg(normalized_tok, '; ' ORDER BY normalized_tok) AS new_value
    FROM normalized
    GROUP BY entity_id
)
UPDATE entity_metadata em
SET corpus_lies = r.new_value
FROM rejoined r
WHERE em.entity_id = r.entity_id;

