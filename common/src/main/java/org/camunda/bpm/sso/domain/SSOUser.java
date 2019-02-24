package org.camunda.bpm.sso.domain;

import lombok.Data;
import org.camunda.bpm.engine.identity.User;

import java.util.HashSet;
import java.util.Set;

@Data
public class SSOUser {

    /**
     * Id
     */
    private String id;

    /**
     * FirstName
     */
    private String firstName;

    /**
     * LastName
     */
    private String lastName;

    /**
     * Email
     */
    private String email;

    /**
     * The currently signed in User
     */
    private User camundaUser;

    /**
     * User Roles
     */
    private Set<String> roles = new HashSet<>();

    /**
     * User tenants
     */
    private Set<String> tenants = new HashSet<>();

    /**
     * Authorized Apps
     */
    private HashSet<String> authorizedApps = new HashSet<>();

}
