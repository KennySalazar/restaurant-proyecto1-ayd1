package com.restaurante.domain.repository;

import com.restaurante.domain.model.ModifierRecipeVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ModifierRecipeVersionRepository extends JpaRepository<ModifierRecipeVersion, Long> {

    @Query("SELECT mrv FROM ModifierRecipeVersion mrv WHERE mrv.modifier.id = :modifierId AND mrv.status = 'VIGENTE'")
    Optional<ModifierRecipeVersion> findActiveByModifierId(@Param("modifierId") Long modifierId);

    @Query("SELECT COUNT(mrv) > 0 FROM ModifierRecipeVersion mrv WHERE mrv.modifier.id = :modifierId AND mrv.status = 'VIGENTE'")
    boolean existsActiveByModifierId(@Param("modifierId") Long modifierId);

    @Query("SELECT COALESCE(MAX(mrv.versionNumber), 0) FROM ModifierRecipeVersion mrv WHERE mrv.modifier.id = :modifierId")
    int findMaxVersionNumberByModifierId(@Param("modifierId") Long modifierId);
}
