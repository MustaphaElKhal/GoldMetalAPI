package com.example.GoldMedalAPI.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import com.example.GoldMedalAPI.model.Country;

public interface CountryRepository extends CrudRepository<Country, Long> {
    Optional<Country> findByName(String name);
    List<Country> findByOrderByNameAsc();
    List<Country> findByOrderByNameDesc();
    List<Country> findByOrderByGdpAsc();
    List<Country> findByOrderByGdpDesc();
    List<Country> findByOrderByPopulationAsc();
    List<Country> findByOrderByPopulationDesc();
}
