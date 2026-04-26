ALTER TABLE description_monnaie
    DROP COLUMN IF EXISTS coins_monetaires_droit,
    DROP COLUMN IF EXISTS coins_monetaires_revers;

ALTER TABLE description_monnaie_aud
    DROP COLUMN IF EXISTS coins_monetaires_droit,
    DROP COLUMN IF EXISTS coins_monetaires_revers;
