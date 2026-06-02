package com.example.GoldMedalAPI.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.GoldMedalAPI.model.CountriesResponse;
import com.example.GoldMedalAPI.model.Country;
import com.example.GoldMedalAPI.model.CountryDetailsResponse;
import com.example.GoldMedalAPI.model.CountryMedalsListResponse;
import com.example.GoldMedalAPI.model.CountrySummary;
import com.example.GoldMedalAPI.model.GoldMedal;
import com.example.GoldMedalAPI.repository.CountryRepository;
import com.example.GoldMedalAPI.repository.GoldMedalRepository;
import org.apache.commons.text.WordUtils;

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

    @GetMapping("/{country}")
    public CountryDetailsResponse getCountryDetails(@PathVariable String country) {
        String countryName = WordUtils.capitalizeFully(country);
        return getCountryDetailsResponse(countryName);
    }

    @GetMapping("/{country}/medals")
    public CountryMedalsListResponse getCountryMedalsList(@PathVariable String country, @RequestParam String sort_by, @RequestParam String ascending) {
        String countryName = WordUtils.capitalizeFully(country);
        var ascendingOrder = ascending.toLowerCase().equals("y");
        return getCountryMedalsListResponse(countryName, sort_by.toLowerCase(), ascendingOrder);
    }

    //region Private methods
    private CountryMedalsListResponse getCountryMedalsListResponse(String countryName, String sortBy, boolean ascendingOrder) {
        List<GoldMedal> medalsList;
        switch (sortBy) {
            case "year":
                medalsList = ascendingOrder ? goldMedalRepository.findByCountryOrderByYearAsc(countryName) : goldMedalRepository.findByCountryOrderByYearDesc(countryName);
                break;
            case "season":
                medalsList = ascendingOrder ? goldMedalRepository.findByCountryOrderBySeasonAsc(countryName) : goldMedalRepository.findByCountryOrderBySeasonDesc(countryName);
                break;
            case "city":
                medalsList = ascendingOrder ? goldMedalRepository.findByCountryOrderByCityAsc(countryName) : goldMedalRepository.findByCountryOrderByCityDesc(countryName);
                break;
            case "name":
                medalsList = ascendingOrder ? goldMedalRepository.findByCountryOrderByNameAsc(countryName) : goldMedalRepository.findByCountryOrderByNameDesc(countryName);
                break;
            case "event":
                medalsList = ascendingOrder ? goldMedalRepository.findByCountryOrderByEventAsc(countryName) : goldMedalRepository.findByCountryOrderByEventDesc(countryName);
                break;
            default:
                medalsList = new ArrayList<>();
                break;
        }

        return new CountryMedalsListResponse(medalsList);
    }

    private CountryDetailsResponse getCountryDetailsResponse(String countryName) {
        var countryOptional = countryRepository.findByName(countryName);
        if (countryOptional.isEmpty()) {
            return new CountryDetailsResponse(countryName);
        }

        var country = countryOptional.get();
        var goldMedalCount = goldMedalRepository.countByCountry(countryName);

        var summerWins = goldMedalRepository.findByCountryAndSeasonOrderByYearAsc(countryName, "Summer");
        var numberSummerWins = summerWins.size() > 0 ? summerWins.size() : null;
        var totalSummerEvents = goldMedalRepository.countBySeason("Summer");
        var percentageTotalSummerWins = totalSummerEvents != 0 && numberSummerWins != null ? (float) summerWins.size() / totalSummerEvents : null;
        var yearFirstSummerWin = summerWins.size() > 0 ? summerWins.get(0).getYear() : null;

        var winterWins = goldMedalRepository.findByCountryAndSeasonOrderByYearAsc(countryName,"Winter");
        var numberWinterWins = winterWins.size() > 0 ? winterWins.size() : null;
        var totalWinterEvents = goldMedalRepository.countBySeason("Winter");
        var percentageTotalWinterWins = totalWinterEvents != 0 && numberWinterWins != null ? (float) winterWins.size() / totalWinterEvents : null;
        var yearFirstWinterWin = winterWins.size() > 0 ? winterWins.get(0).getYear() : null;

        var numberEventsWonByFemaleAthletes = goldMedalRepository.countByCountryAndGender(countryName, "Women");
        var numberEventsWonByMaleAthletes = goldMedalRepository.countByCountryAndGender(countryName,"Men");

        return new CountryDetailsResponse(
                countryName,
                country.getGdp(),
                country.getPopulation(),
                goldMedalCount,
                numberSummerWins,
                percentageTotalSummerWins,
                yearFirstSummerWin,
                numberWinterWins,
                percentageTotalWinterWins,
                yearFirstWinterWin,
                numberEventsWonByFemaleAthletes,
                numberEventsWonByMaleAthletes);
    }

    private List<CountrySummary> getCountrySummaries(String sortBy, boolean ascendingOrder) {
        List<Country> countries;
        switch (sortBy) {
            case "name":
                countries = ascendingOrder ? countryRepository.findByOrderByNameAsc() : countryRepository.findByOrderByNameDesc();
                break;
            case "gdp":
                countries = ascendingOrder ? countryRepository.findByOrderByGdpAsc() : countryRepository.findByOrderByGdpDesc();
                break;
            case "population":
                countries = ascendingOrder ? countryRepository.findByOrderByPopulationAsc() : countryRepository.findByOrderByPopulationDesc();
                break;
            case "medals":
            default:
                countries = countryRepository.findByOrderByNameAsc();
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
            var goldMedalCount = goldMedalRepository.countByCountry(country.getName());
            countrySummaries.add(new CountrySummary(country, goldMedalCount));
        }
        return countrySummaries;
    }
    //endregion
}
