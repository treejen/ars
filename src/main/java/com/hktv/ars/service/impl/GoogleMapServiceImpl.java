package com.hktv.ars.service.impl;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.AddressComponent;
import com.google.maps.model.AddressComponentType;
import com.google.maps.model.GeocodingResult;
import com.hktv.ars.data.RegionResponseData;
import com.hktv.ars.service.AddressExtractionService;
import com.hktv.ars.service.KnnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleMapServiceImpl implements AddressExtractionService {

    private final KnnService knnService;

    private static final GeoApiContext geoApiContext = new GeoApiContext.Builder()
            .apiKey("AIzaSyANFvDmQP-1mM_97xn1JpU9q6rAKWp4LVo")
            .build();

    @Override
    public RegionResponseData extractAddresses(String address) {
        String street = null;
        String district = null;
        String estate = null;
        String number = null;
        String deliveryZoneCode = null;
        double latitude = 0d;
        double longitude = 0d;
        try {
            GeocodingResult[] results = GeocodingApi.geocode(geoApiContext, address).await();
            if (results.length > 0) {
                latitude = results[0].geometry.location.lat;
                longitude = results[0].geometry.location.lng;
                AddressComponent[] components = results[0].addressComponents;
                street = getComponentValue(components, AddressComponentType.ROUTE.toString()).orElse(null); //皇后大道中
                number = normalizeStreetNumber(getComponentValue(components, AddressComponentType.STREET_NUMBER.toString()).orElse(null)); //99
                estate = getComponentValue(components, AddressComponentType.PREMISE.toString()).orElse(null); //中環中心
                district = getComponentValue(components, AddressComponentType.NEIGHBORHOOD.toString()).orElse(null); //中環
                deliveryZoneCode = knnService.predict(latitude, longitude);
            }
        } catch (IOException | ApiException | InterruptedException ex) {
            log.error(ex.getMessage());
        }

        return RegionResponseData.builder()
                .address(address)
                .deliveryZoneCode(deliveryZoneCode)
                .latitude(BigDecimal.valueOf(latitude))
                .longitude(BigDecimal.valueOf(longitude))
                .estate(estate)
                .dist(district)
                .street(street)
                .number(number)
                .build();
    }

    // 從 addressComponents 中提取特定 type 的 long_name
    private static Optional<String> getComponentValue(AddressComponent[] components, String type) {
        return Arrays.stream(components)
                .filter(c -> Arrays.stream(c.types)
                        .map(Enum::name)
                        .anyMatch(t -> t.equalsIgnoreCase(type)))
                .map(c -> c.longName)
                .findFirst();
    }

    private static String normalizeStreetNumber(String streetNumber) {
        return streetNumber == null? null : streetNumber.replaceAll("[^0-9]", ""); // 只保留數字
    }

}
