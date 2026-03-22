package com.huashi.eftransfer.app.modules.lexicon.imports.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huashi.eftransfer.app.modules.lexicon.imports.entity.LexicalImportFileEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LexicalImportFileMapper extends BaseMapper<LexicalImportFileEntity> {

    @Select("""
            SELECT id, batch_id, original_filename, content_type, file_size_bytes, sha256, file_content,
                   created_at, created_by, updated_at, updated_by, deleted
            FROM lexical_import_file
            WHERE deleted = FALSE
              AND batch_id = #{batchId}
            LIMIT 1
            """)
    LexicalImportFileEntity selectByBatchId(@Param("batchId") Long batchId);
}
