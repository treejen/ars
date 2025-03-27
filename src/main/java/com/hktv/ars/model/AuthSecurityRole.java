package com.hktv.ars.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@Builder
@Entity
@Table(name = "auth_security_role")
@AllArgsConstructor
@NoArgsConstructor
public class AuthSecurityRole extends BaseModel {

    @NotBlank(message = "auth security role name can't be blank.")
    @Pattern(regexp = "^ROLE_.*?", message = "prefix must be ROLE_")
    private String name;

    @OneToMany(
            fetch = FetchType.LAZY,
            mappedBy = "authSecurityRole",
            cascade = {CascadeType.PERSIST}
    )
    private Set<AuthSecurityApiRole> authSecurityApiRoles;
}
