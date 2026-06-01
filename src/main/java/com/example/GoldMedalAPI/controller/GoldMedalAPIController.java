package com.example.GoldMedalAPI.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.GoldMedalAPI.model.CountriesResponse;
import com.example.GoldMedalAPI.model.Country;
import com.example.GoldMedalAPI.model.CountrySummary;
import com.example.GoldMedalAPI.repository.CountryRepository;
import com.example.GoldMedalAPI.repository.GoldMedalRepository;

@RestController
@RequestMapping("/countries")
public class GoldMedalAPIController {
    private final GoldMedalRepository goldMedalRepository;
    private final CountryRepository countryRepository;

    public GoldMedalAPIController(
        final GoldMedalRepository goldMedalRepository,
        final CountryRepository countryRepository)
    {
        this.goldMedalRepository = goldMedalRepository;
        this.countryRepository = countryRepository;
    }

    @GetMapping
    public CountriesResponse getCountries(@RequestParam String sort_by, @RequestParam String ascending) {
        var ascendingOrder = ascending.toLowerCase().equals("y");
        return new CountriesResponse(getCountrySummaries(sort_by.toLowerCase(), ascendingOrder));
    }

    private List<CountrySummary> getCountrySummaries(String sortBy, boolean ascendingOrder) {
        List<Country> countries;
        switch (sortBy) {
            case "gdp":
                countries = ascendingOrder ? this.countryRepository.findByOrderByGdpAsc() : this.countryRepository.findByOrderByGdpDesc();
                break;
            case "population":
                countries = ascendingOrder ? this.countryRepository.findByOrderByPopulationAsc() : this.countryRepository.findByOrderByPopulationDesc();
                break;
            case "medals": // additional logic below will handle that
            case "name":
            default:
                countries = ascendingOrder ? this.countryRepository.findByOrderByNameAsc() : this.countryRepository.findByOrderByNameDesc();
                break;
        }

        var countrySummaries = getCountrySummariesWithMedalCount(countries);

        if (sortBy.equalsIgnoreCase("medals")) {
            countrySummaries = sortByMedalCount(countrySummaries, ascendingOrder);
        }

        return countrySummaries;
    }

    private List<CountrySummary> sortByMedalCount(List<CountrySummary> countrySummaries, boolean ascendingOrder) {
        return countrySummaries.stream()
                .sorted((t1, t2) -> ascendingOrder ?
                        t1.getMedals() - t2.getMedals() :
                        t2.getMedals() - t1.getMedals())
                .collect(Collectors.toList());
    }

    private List<CountrySummary> getCountrySummariesWithMedalCount(List<Country> countries) {
        List<CountrySummary> countrySummaries = new ArrayList<>();
        for (var country : countries) {
            var goldMedalCount = this.goldMedalRepository.findByCountry(country.getName()).size();
            countrySummaries.add(new CountrySummary(country, goldMedalCount));
        }
        return countrySummaries;
    }
}
