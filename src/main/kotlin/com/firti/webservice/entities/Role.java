package com.firti.webservice.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.firti.webservice.entities.base.BaseEntity;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "roles")
public class Role extends BaseEntity {
    private String name;
    private String role;

    public Role() {
    }

    public Role(ERole eRole) {
        this.name = eRole.getValue();
        this.role = eRole.toString();
    }


    public enum ERole {
        ROLE_ADMIN("Admin"),
        ROLE_EMPLOYEE("Employee"),
        ROLE_FIELD_EMPLOYEE("Field Employee"),
        ROLE_LANDLORD("LandLord"),
        ROLE_USER("User");

        String value;

        ERole(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static ERole getERole(String role) {
        for (ERole eRole : ERole.values()) {
            if (eRole.toString().equals(role))
                return eRole;
        }
        return ERole.ROLE_USER;
    }

    public static ERole getERoleFromRoleName(String roleName) {
        for (ERole eRole : ERole.values()) {
            if (eRole.getValue().trim().toLowerCase().equals(roleName.trim().toLowerCase()))
                return eRole;
        }
        return ERole.ROLE_USER;
    }

    public static class StringRole {
        public static final String ROLE_USER = "ROLE_USER";
        public static final String ROLE_EMPLOYEE = "ROLE_EMPLOYEE";
        public static final String ROLE_FIELD_EMPLOYEE = "ROLE_FIELD_EMPLOYEE";
        public static final String ROLE_LANDLORD = "ROLE_LANDLORD";
        public static final String ROLE_ADMIN = "ROLE_ADMIN";
    }

    @JsonIgnore
    public boolean isAdmin() {
        return this.role != null && this.role.equals(ERole.ROLE_ADMIN.toString());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
