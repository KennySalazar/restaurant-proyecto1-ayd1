package com.restaurante.application.table;

import com.restaurante.domain.model.RestaurantTable;
import com.restaurante.domain.model.RestaurantUserProfile;
import com.restaurante.domain.model.TableZone;
import com.restaurante.domain.repository.RestaurantTableRepository;
import com.restaurante.domain.repository.RestaurantUserProfileRepository;
import com.restaurante.domain.repository.TableZoneRepository;
import com.restaurante.exception.ApiException;
import com.restaurante.security.JwtData;
import com.restaurante.web.dto.table.CreateTableRequest;
import com.restaurante.web.dto.table.TableResponse;
import com.restaurante.web.dto.table.TableZoneResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.restaurante.web.dto.table.UpdateTableRequest;

import java.util.List;

@Service
public class TableService {

    private final RestaurantTableRepository tables;
    private final TableZoneRepository zones;
    private final RestaurantUserProfileRepository profiles;

    public TableService(RestaurantTableRepository tables,
                        TableZoneRepository zones,
                        RestaurantUserProfileRepository profiles) {
        this.tables = tables;
        this.zones = zones;
        this.profiles = profiles;
    }

    @Transactional
    public TableResponse createTable(CreateTableRequest request, Authentication authentication) {
        Long restaurantId = getRestaurantId(authentication);
        String number = request.numero().trim();

        if (tables.existsByRestaurantIdAndNumber(restaurantId, number)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "table_number_in_use",
                    "Número de mesa en uso",
                    "El número de mesa ya está en uso"
            );
        }

        TableZone zone = zones
                .findByIdAndRestaurantIdAndActiveTrue(request.zonaId(), restaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "invalid_table_zone",
                        "Zona de mesa inválida",
                        "La zona seleccionada no existe o no está disponible"
                ));

        RestaurantTable table = new RestaurantTable(
                restaurantId,
                zone,
                number,
                request.capacidad(),
                request.estadoInicial()
        );

        RestaurantTable saved = tables.save(table);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TableZoneResponse> getAvailableZones(Authentication authentication) {
        Long restaurantId = getRestaurantId(authentication);

        return zones.findAllByRestaurantIdAndActiveTrueOrderByIdAsc(restaurantId)
                .stream()
                .map(zone -> new TableZoneResponse(
                        zone.getId(),
                        zone.getName()
                ))
                .toList();
    }

    private Long getRestaurantId(Authentication authentication) {
        if (authentication == null || !(authentication.getDetails() instanceof JwtData jwtData)) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "invalid_authenticated_user",
                    "Usuario no autenticado",
                    "No fue posible identificar al usuario autenticado"
            );
        }

        RestaurantUserProfile profile = profiles.findById(jwtData.userId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.CONFLICT,
                        "restaurant_profile_not_found",
                        "Perfil de restaurante no encontrado",
                        "El usuario autenticado no tiene un perfil asociado al restaurante"
                ));

        return profile.getRestaurantId();
    }

    private TableResponse toResponse(RestaurantTable table) {
        return new TableResponse(
                table.getId(),
                table.getNumber(),
                table.getCapacity(),
                new TableZoneResponse(
                        table.getZone().getId(),
                        table.getZone().getName()
                ),
                table.getStatus(),
                table.isActive()
        );
    }

    @Transactional
    public TableResponse updateTable(Long tableId,
                                     UpdateTableRequest request,
                                     Authentication authentication) {

        Long restaurantId = getRestaurantId(authentication);
        String number = request.numero().trim();

        RestaurantTable table = tables
                .findByIdAndRestaurantIdAndActiveTrue(tableId, restaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "table_not_found",
                        "Mesa no encontrada",
                        "La mesa seleccionada no existe o no está activa"
                ));

        if (tables.existsByRestaurantIdAndNumberAndIdNot(
                restaurantId,
                number,
                tableId)) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "table_number_in_use",
                    "Número de mesa en uso",
                    "El número de mesa ya está en uso"
            );
        }

        TableZone zone = zones
                .findByIdAndRestaurantIdAndActiveTrue(
                        request.zonaId(),
                        restaurantId
                )
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "invalid_table_zone",
                        "Zona de mesa inválida",
                        "La zona seleccionada no existe o no está disponible"
                ));

        table.updateConfiguration(
                number,
                request.capacidad(),
                zone
        );

        RestaurantTable saved = tables.save(table);

        return toResponse(saved);
    }
}