package com.wildbugs.freshroute.repository;

import com.wildbugs.freshroute.model.DestinationMarket;
import com.wildbugs.freshroute.model.enums.DestinationTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DestinationMarketRepository extends JpaRepository<DestinationMarket, Long> {

    List<DestinationMarket> findByTier(DestinationTier tier);

    Optional<DestinationMarket> findByName(String name);
}
