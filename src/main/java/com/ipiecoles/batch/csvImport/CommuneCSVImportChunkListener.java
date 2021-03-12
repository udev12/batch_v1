package com.ipiecoles.batch.csvImport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.scope.context.ChunkContext;

public class CommuneCSVImportChunkListener implements ChunkListener {

    Logger logger = LoggerFactory.getLogger(this.getClass());
    @Override
    public void beforeChunk(ChunkContext chunkContext) {
        logger.info("Before Chunk CSV Import");
    }

    @Override
    public void afterChunk(ChunkContext chunkContext) {
        logger.info("After Chunk CSV Import");
    }

    @Override
    public void afterChunkError(ChunkContext context) {
        logger.error("After Chunk Error CSV Import");
    }
}
