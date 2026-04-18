ALTER TABLE lexical_pair
    ADD COLUMN active_english_word VARCHAR(128)
        GENERATED ALWAYS AS (CASE WHEN deleted = FALSE THEN english_word ELSE NULL END)
/*!80000 , ALGORITHM=INSTANT */;

ALTER TABLE lexical_pair
    ADD UNIQUE INDEX uk_lexical_pair_word_pair_active (active_english_word, french_word)
/*!80000 , ALGORITHM=INPLACE, LOCK=NONE */;

ALTER TABLE lexical_pair
    DROP INDEX uk_lexical_pair_word_pair
/*!80000 , ALGORITHM=INPLACE, LOCK=NONE */;

ALTER TABLE lexical_tag
    ADD COLUMN active_tag_name VARCHAR(64)
        GENERATED ALWAYS AS (CASE WHEN deleted = FALSE THEN tag_name ELSE NULL END)
/*!80000 , ALGORITHM=INSTANT */;

ALTER TABLE lexical_tag
    ADD UNIQUE INDEX uk_lexical_tag_name_active (active_tag_name)
/*!80000 , ALGORITHM=INPLACE, LOCK=NONE */;

ALTER TABLE lexical_tag
    DROP INDEX uk_lexical_tag_name
/*!80000 , ALGORITHM=INPLACE, LOCK=NONE */;

ALTER TABLE lexical_pair_tag_rel
    ADD COLUMN active_lexical_pair_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN deleted = FALSE THEN lexical_pair_id ELSE NULL END)
/*!80000 , ALGORITHM=INSTANT */;

ALTER TABLE lexical_pair_tag_rel
    ADD UNIQUE INDEX uk_lexical_pair_tag_rel_active (active_lexical_pair_id, lexical_tag_id)
/*!80000 , ALGORITHM=INPLACE, LOCK=NONE */;

ALTER TABLE lexical_pair_tag_rel
    DROP INDEX uk_lexical_pair_tag_rel
/*!80000 , ALGORITHM=INPLACE, LOCK=NONE */;

ALTER TABLE lexical_list_item
    ADD COLUMN active_lexical_list_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN deleted = FALSE THEN lexical_list_id ELSE NULL END)
/*!80000 , ALGORITHM=INSTANT */;

ALTER TABLE lexical_list_item
    ADD UNIQUE INDEX uk_lexical_list_item_active (active_lexical_list_id, lexical_pair_id)
/*!80000 , ALGORITHM=INPLACE, LOCK=NONE */;

ALTER TABLE lexical_list_item
    DROP INDEX uk_lexical_list_item
/*!80000 , ALGORITHM=INPLACE, LOCK=NONE */;

ALTER TABLE lexical_import_file
    ADD COLUMN active_batch_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN deleted = FALSE THEN batch_id ELSE NULL END)
/*!80000 , ALGORITHM=INSTANT */;

ALTER TABLE lexical_import_file
    ADD UNIQUE INDEX uk_lexical_import_file_batch_active (active_batch_id)
/*!80000 , ALGORITHM=INPLACE, LOCK=NONE */;

ALTER TABLE lexical_import_file
    DROP INDEX uk_lexical_import_file_batch
/*!80000 , ALGORITHM=INPLACE, LOCK=NONE */;

ALTER TABLE lexical_import_row
    ADD COLUMN active_batch_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN deleted = FALSE THEN batch_id ELSE NULL END)
/*!80000 , ALGORITHM=INSTANT */;

ALTER TABLE lexical_import_row
    ADD UNIQUE INDEX uk_lexical_import_row_batch_row_active (active_batch_id, import_row_number)
/*!80000 , ALGORITHM=INPLACE, LOCK=NONE */;

ALTER TABLE lexical_import_row
    DROP INDEX uk_lexical_import_row_batch_row
/*!80000 , ALGORITHM=INPLACE, LOCK=NONE */;
