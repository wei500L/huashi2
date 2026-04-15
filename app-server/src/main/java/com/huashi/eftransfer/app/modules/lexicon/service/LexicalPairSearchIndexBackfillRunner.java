package com.huashi.eftransfer.app.modules.lexicon.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class LexicalPairSearchIndexBackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LexicalPairSearchIndexBackfillRunner.class);

    private final LexicalPairService lexicalPairService;

    public LexicalPairSearchIndexBackfillRunner(LexicalPairService lexicalPairService) {
        this.lexicalPairService = lexicalPairService;
    }

    @Override
    public void run(ApplicationArguments args) {
        int updated = lexicalPairService.backfillMissingSearchIndexes();
        if (updated > 0) {
            log.info("event=lexical_pair_search_index_backfilled updated={}", updated);
        }
    }
}
