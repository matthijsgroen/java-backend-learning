package nl.kabisa.dashboarding.widget.orm;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WidgetRepository extends JpaRepository<Widget, UUID> {

    /**
     * Find all direct children of a widget.
     */
    @Query("SELECT w FROM Widget w WHERE w.parent.id = :parentId")
    List<Widget> findByParentId(@Param("parentId") UUID parentId);

    /**
     * Find all direct children's IDs (lightweight - no full entity load).
     */
    @Query("SELECT w.id FROM Widget w WHERE w.parent.id = :parentId")
    List<UUID> findChildIdsByParentId(@Param("parentId") UUID parentId);

    /**
     * Find all descendant IDs recursively using PostgreSQL recursive CTE.
     * Used for cascade delete.
     * Returns List<String> because JDBC returns UUIDs as strings for native queries.
     */
    @Query(value = """
            WITH RECURSIVE descendants AS (
                SELECT id FROM widgets WHERE parent_id = :rootId
                UNION ALL
                SELECT w.id FROM widgets w
                INNER JOIN descendants d ON w.parent_id = d.id
            )
            SELECT id FROM descendants
            """, nativeQuery = true)
    List<String> findAllDescendantIds(@Param("rootId") UUID rootId);

    /**
     * Find all ancestor IDs by walking up the parent chain.
     * Used for cycle detection.
     * Returns List<String> because JDBC returns UUIDs as strings for native queries.
     */
    @Query(value = """
            WITH RECURSIVE ancestors AS (
                SELECT parent_id FROM widgets WHERE id = :startId
                UNION ALL
                SELECT w.parent_id FROM widgets w
                INNER JOIN ancestors a ON w.id = a.parent_id
                WHERE a.parent_id IS NOT NULL
            )
            SELECT parent_id FROM ancestors WHERE parent_id IS NOT NULL
            """, nativeQuery = true)
    List<String> findAllAncestorIds(@Param("startId") UUID startId);

    /**
     * Delete a widget and all its descendants atomically using a recursive CTE.
     * Returns the number of deleted rows.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            WITH RECURSIVE descendants AS (
                SELECT id FROM widgets WHERE id = :rootId
                UNION ALL
                SELECT w.id FROM widgets w
                INNER JOIN descendants d ON w.parent_id = d.id
            )
            DELETE FROM widgets WHERE id IN (SELECT id FROM descendants)
            """, nativeQuery = true)
    int deleteWidgetTree(@Param("rootId") UUID rootId);
}
