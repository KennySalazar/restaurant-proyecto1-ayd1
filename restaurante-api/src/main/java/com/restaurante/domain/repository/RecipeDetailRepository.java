package com.restaurante.domain.repository;

import com.restaurante.domain.model.RecipeDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RecipeDetailRepository extends JpaRepository<RecipeDetail, Long> {

    List<RecipeDetail> findByRecipeVersionId(Long recipeVersionId);
}
