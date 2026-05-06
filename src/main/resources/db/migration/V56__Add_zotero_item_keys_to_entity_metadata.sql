-- Références Zotero (groupe partagé) : liste d'item keys JSON ["ABC","DEF"]
ALTER TABLE entity_metadata ADD COLUMN IF NOT EXISTS zotero_item_keys TEXT;

ALTER TABLE entity_metadata_aud ADD COLUMN IF NOT EXISTS zotero_item_keys TEXT;
