package com.travel.insurance.department;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    Optional<Department> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);

    /**
     * Case-insensitive bulk lookup by trimmed name. {@code names} must already be
     * lower-cased and trimmed by the caller.
     */
    @Query("select d from Department d where lower(trim(d.name)) in :names")
    List<Department> findByLowerTrimmedNameIn(@Param("names") Collection<String> names);
}
