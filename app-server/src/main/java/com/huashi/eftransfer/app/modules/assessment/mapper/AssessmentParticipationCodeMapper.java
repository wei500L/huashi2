package com.huashi.eftransfer.app.modules.assessment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentParticipationCodeEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AssessmentParticipationCodeMapper extends BaseMapper<AssessmentParticipationCodeEntity> {

    @Select("""
            SELECT id,public_release_id,code_digest,code_hint,status,export_batch_id,exported_at,first_verified_at,
                   last_verified_at,submitted_at,created_at,created_by,updated_at,updated_by,deleted
            FROM assessment_participation_code
            WHERE public_release_id = #{releaseId}
              AND code_digest = #{digest}
              AND deleted = FALSE
            LIMIT 1
            FOR UPDATE
            """)
    AssessmentParticipationCodeEntity selectByReleaseAndDigestForUpdate(
            @Param("releaseId") Long releaseId,
            @Param("digest") String digest
    );

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM assessment_participation_code
            WHERE public_release_id = #{releaseId}
              AND deleted = FALSE
            <if test="status != null">
              AND status = #{status}
            </if>
            <choose>
              <when test="legacyBatch">
                AND export_batch_id IS NULL
              </when>
              <when test="batchId != null">
                AND export_batch_id = #{batchId}
              </when>
            </choose>
            </script>
            """)
    long countForManagement(
            @Param("releaseId") Long releaseId,
            @Param("status") String status,
            @Param("batchId") String batchId,
            @Param("legacyBatch") boolean legacyBatch
    );

    @Select("""
            <script>
            SELECT id,public_release_id,code_digest,code_hint,status,export_batch_id,exported_at,first_verified_at,
                   last_verified_at,submitted_at,created_at,created_by,updated_at,updated_by,deleted
            FROM assessment_participation_code
            WHERE public_release_id = #{releaseId}
              AND deleted = FALSE
            <if test="status != null">
              AND status = #{status}
            </if>
            <choose>
              <when test="legacyBatch">
                AND export_batch_id IS NULL
              </when>
              <when test="batchId != null">
                AND export_batch_id = #{batchId}
              </when>
            </choose>
            ORDER BY id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<AssessmentParticipationCodeEntity> selectManagementPage(
            @Param("releaseId") Long releaseId,
            @Param("status") String status,
            @Param("batchId") String batchId,
            @Param("legacyBatch") boolean legacyBatch,
            @Param("limit") int limit,
            @Param("offset") long offset
    );

    @Insert("""
            <script>
            INSERT INTO assessment_participation_code
                (public_release_id,code_digest,code_hint,status,export_batch_id,exported_at)
            VALUES
            <foreach collection="codes" item="code" separator=",">
                (#{code.publicReleaseId},#{code.codeDigest},#{code.codeHint},#{code.status},
                 #{code.exportBatchId},#{code.exportedAt})
            </foreach>
            </script>
            """)
    int insertBatch(@Param("codes") List<AssessmentParticipationCodeEntity> codes);

    @Update("""
            UPDATE assessment_participation_code
            SET status = 'REVOKED', updated_at = CURRENT_TIMESTAMP
            WHERE id = #{codeId}
              AND public_release_id = #{releaseId}
              AND status = 'UNUSED'
              AND deleted = FALSE
            """)
    int revokeUnused(@Param("releaseId") Long releaseId, @Param("codeId") Long codeId);
}
