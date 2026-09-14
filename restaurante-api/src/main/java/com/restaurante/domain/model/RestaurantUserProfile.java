package com.restaurante.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "usuarios", schema = "restaurante")
public class RestaurantUserProfile {

    @Id
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restaurantId;

    @Column(name = "codigo_empleado", nullable = false, length = 30)
    private String employeeCode;

    @Column(name = "nombres", nullable = false, length = 100)
    private String firstName;

    @Column(name = "apellidos", nullable = false, length = 100)
    private String lastName;

    @Column(name = "fecha_contratacion")
    private LocalDate hireDate;

    protected RestaurantUserProfile() {
    }

    public RestaurantUserProfile(Long id, Long restaurantId, String employeeCode,
                                 String firstName, String lastName, LocalDate hireDate) {
        this.id = id;
        this.restaurantId = restaurantId;
        this.employeeCode = employeeCode;
        this.firstName = firstName;
        this.lastName = lastName;
        this.hireDate = hireDate;
    }

    public Long getId() {
        return id;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }
}
