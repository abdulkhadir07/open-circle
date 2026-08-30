package com.opencircle.location;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;

@Component
class NominatimLocationResolver implements LocationResolver {

    private final RestClient restClient;

    NominatimLocationResolver(RestClient.Builder restClientBuilder, LocationProperties properties) {
        this.restClient = restClientBuilder
                .baseUrl(properties.getNominatimBaseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, properties.getUserAgent())
                .build();
    }

    @Override
    public ResolvedLocation resolve(BigDecimal latitude, BigDecimal longitude) {
        NominatimResponse response = fetchLocation(latitude, longitude);

        if (response == null || response.address() == null) {
            throw new LocationResolutionException();
        }

        String city = firstPresent(
                response.address().city(),
                response.address().town(),
                response.address().village(),
                response.address().municipality(),
                response.address().hamlet(),
                response.address().county()
        );

        String stateRegion = firstPresent(
                response.address().state(),
                response.address().province(),
                response.address().region(),
                response.address().stateDistrict()
        );

        String country = blankToNull(response.address().country());

        if (city == null || country == null) {
            throw new LocationResolutionException();
        }

        return new ResolvedLocation(city, stateRegion, country);
    }

    private NominatimResponse fetchLocation(BigDecimal latitude, BigDecimal longitude) {
        try {
            // Reverse-geocodes device coordinates into address fields without storing the raw coordinates.
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/reverse")
                            .queryParam("format", "jsonv2")
                            .queryParam("lat", latitude)
                            .queryParam("lon", longitude)
                            .queryParam("zoom", 10)
                            .queryParam("addressdetails", 1)
                            .queryParam("accept-language", "en")
                            .build())
                    .retrieve()
                    .body(NominatimResponse.class);
        } catch (RestClientException exception) {
            throw new LocationServiceUnavailableException();
        }
    }

    private String firstPresent(String... values) {
        for (String value : values) {
            String normalized = blankToNull(value);

            if (normalized != null) {
                return normalized;
            }
        }

        return null;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NominatimResponse(
            NominatimAddress address
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NominatimAddress(
            String city,
            String town,
            String village,
            String municipality,
            String hamlet,
            String county,
            String state,
            String province,
            String region,
            @JsonProperty("state_district")
            String stateDistrict,
            String country
    ) {
    }
}
