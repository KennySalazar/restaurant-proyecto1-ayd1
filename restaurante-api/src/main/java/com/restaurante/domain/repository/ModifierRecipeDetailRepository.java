package com.restaurante.domain.repository;

import com.restaurante.domain.model.ModifierRecipeDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModifierRecipeDetailRepository extends JpaRepository<ModifierRecipeDetail, Long> {

    List<ModifierRecipeDetail> findByModifierRecipeVersionId(Long versionId);
}
