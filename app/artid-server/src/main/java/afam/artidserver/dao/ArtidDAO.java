package afam.artidserver.dao;

import afam.artidserver.model.entity.Artid;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArtidDAO extends ListCrudRepository<Artid, Long> {

        long countByIdUserAndDeletedAtIsNull(Long idUser);

        List<Artid> findAllByIdUserAndDeletedAtIsNullOrderByLastModifiedDesc(Long idUser);

        // Ownership a livello di query: l'id_user è nel WHERE, quindi un ArtID di un
        // altro
        // utente (o inesistente / soft-deleted) non viene MAI restituito → 404
        // indistinguibile.
        Optional<Artid> findByIdAndIdUserAndDeletedAtIsNull(Long id, Long idUser);

        @Query("SELECT COUNT(*) FROM artid_resource WHERE id = :artidId")
        long countResourcesByArtidId(@Param("artidId") Long artidId);

        @Query("SELECT COUNT(*) > 0 FROM artid_resource WHERE id = :artidId AND id_resource = :resourceId")
        boolean existsResourceInArtid(@Param("artidId") Long artidId, @Param("resourceId") Long resourceId);

        @Modifying
        @Query("""
                        DELETE FROM artid_resource
                        WHERE id = :artidId AND id_resource = :resourceId
                        """)
        int removeResourceByResourceId(@Param("artidId") Long id, @Param("resourceId") Long resourceId);

        @Query("SELECT rank FROM artid_resource WHERE id = :artidId AND id_resource = :resourceId")
        Optional<Integer> findRankByArtidIdAndResourceId(@Param("artidId") Long artidId,
                        @Param("resourceId") Long resourceId);

        @Modifying
        @Query("UPDATE artid_resource SET rank = rank - 1 WHERE id = :artidId AND rank > :deletedRank")
        void decrementRanksAfterDeletion(@Param("artidId") Long artidId, @Param("deletedRank") Integer deletedRank);

        // 2. Spostamento verso il basso: shifta in su (-1) gli elementi intermedi
        @Modifying
        @Query("""
                        UPDATE artid_resource
                        SET rank = rank - 1
                        WHERE id = :artidId AND rank > :oldRank AND rank <= :newRank
                        """)
        void shiftRanksUp(@Param("artidId") Long artidId, @Param("oldRank") Integer oldRank,
                        @Param("newRank") Integer newRank);

        // 3. Spostamento verso l'alto: shifta in giù (+1) gli elementi intermedi
        @Modifying
        @Query("""
                        UPDATE artid_resource
                        SET rank = rank + 1
                        WHERE id = :artidId AND rank >= :newRank AND rank < :oldRank
                        """)
        void shiftRanksDown(@Param("artidId") Long artidId, @Param("oldRank") Integer oldRank,
                        @Param("newRank") Integer newRank);

        // 4. Aggiorna infine il rank del materiale spostato
        @Modifying
        @Query("UPDATE artid_resource SET rank = :newRank WHERE id = :artidId AND id_resource = :resourceId")
        void updateResourceRank(@Param("artidId") Long artidId, @Param("resourceId") Long resourceId,
                        @Param("newRank") Integer newRank);

        @Modifying
        @Query("""
                        DELETE FROM artid_tag
                        WHERE id_artid = :artidId AND id_tag = :tagId
                        """)
        int removeTagByTagId(@Param("artidId") Long id, @Param("tagId") Long tagId);

}
