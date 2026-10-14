package com.wildbugs.freshroute.repository;

import com.wildbugs.freshroute.model.DestinationMarket;
import com.wildbugs.freshroute.model.TransportRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransportRouteRepository extends JpaRepository<TransportRoute, Long> {

    List<TransportRoute> findByDestinationMarket(DestinationMarket destinationMarket);

    Optional<TransportRoute> findByDestinationMarketId(Long destinationMarketId);
}
