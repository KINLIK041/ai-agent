package com.kinlik.aicodehelper.repository;

import com.kinlik.aicodehelper.entity.MemoryFragment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemoryFragmentRepository extends JpaRepository<MemoryFragment, Long> {
    List<MemoryFragment> findByUsernameOrderByCreatedAtDesc(String username);

    List<MemoryFragment> findByUsernameAndCategory(String username, String category);

    Optional<MemoryFragment> findTopByUsernameAndCategoryOrderByCreatedAtDesc(String username, String category);
}
