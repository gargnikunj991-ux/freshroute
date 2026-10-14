package com.wildbugs.freshroute.repository;

import com.wildbugs.freshroute.model.DispatchAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DispatchAllocationRepository extends JpaRepository<DispatchAllocation, Long> {

    List<DispatchAllocation> findByPlanType(String planType);

    void deleteByPlanType(String planType);
}
