ALTER TABLE lexical_pair
    ADD COLUMN search_pinyin VARCHAR(1024) NULL AFTER searchable_text,
    ADD COLUMN search_initials VARCHAR(255) NULL AFTER search_pinyin;
