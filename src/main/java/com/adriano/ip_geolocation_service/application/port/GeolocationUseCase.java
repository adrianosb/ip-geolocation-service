package com.adriano.ip_geolocation_service.application.port;

import com.adriano.ip_geolocation_service.application.model.GeolocationResponse;
import org.springframework.lang.NonNull;

public interface GeolocationUseCase {

    GeolocationResponse locate(@NonNull String ip);
}
