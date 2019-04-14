package org.camunda.community.sso.keycloak;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.servlet.ServletRequest;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.identity.User;
import org.camunda.community.sso.CamundaSSOCore;
import org.camunda.community.sso.domain.SSOUser;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessToken.Access;

import lombok.Data;

@Data
@Slf4j
public class CamundaSSOKeycloak extends CamundaSSOCore {

    /**
     * Access Token
     */
    private AccessToken token;
    
    /**
     * Keycloak System Clients
     */
    List<String> keycloakNativeClients = new ArrayList<>(Arrays.asList(
        "account",
        "admin-cli",
        "broker",
        "realm-management",
        "security-admin-console"
    ));
    
    /**
     * Constructor
     * 
     * @param request ServletRequest
     */
    public CamundaSSOKeycloak(ServletRequest request) {
        super(request);
    }

    /**
     * Gets the UserName from KeyCloak
     * 
     * @return String The User Name
     */
    public Optional<SSOUser> obtainUserInformation() {
        // Check if camunda is initialized
        if (processEngine == null) {
            log.error("Camunda BPM has not been initialized yet ... Authentication failed!");
            return Optional.empty();
        }

        // Query keycloak
        if (token == null) {
        	token = ((KeycloakPrincipal) getHelper().getReq().getUserPrincipal()).getKeycloakSecurityContext().getToken();
        }

        SSOUser user = new SSOUser();
        user.setId(token.getPreferredUsername());
        user.setFirstName(token.getGivenName());
        user.setLastName(token.getFamilyName());
        user.setEmail(token.getEmail());
        return Optional.ofNullable(user);
    }

    /**
     * Processes the Request using available informations
     */
    public void process(SSOUser user) {
        // Check if camunda is initialized
        if (processEngine == null) {
            log.error("Camunda BPM has not been initialized yet ... Authentication failed!");
            return;
        }

        user.setRoles(new HashSet<>());
        try {
        	Map<String, Access> resourceAccess = getToken().getResourceAccess();
        	
        	for (Entry<String, Access> entry : resourceAccess.entrySet()) {
        		// skip system resources
        		if (getKeycloakNativeClients().contains(entry.getKey())) {
        			continue;
        		}
        		
        		log.info("User [{}] has the following roles [{}] in client [{}]!", user.getId(), entry.getValue().getRoles(), entry.getKey());
        		
        		Set<String> userRoles = entry.getValue().getRoles();
        		for(String role : userRoles) {
                    user.getRoles().add(role);
        		}
        	}
        } catch (Exception ex) {
        	// should never fail, please report if it does
        	ex.printStackTrace();
        }
        
        try {
        	Set<String> userRealmRoles = getToken().getRealmAccess().getRoles();
            user.getRoles().addAll(userRealmRoles);
            
            log.info(String.format("Detected Realm Roles [%s]!", userRealmRoles));
        } catch (Exception ex) {
        	// will fail if the client can't access realm scoped roles in keycloak
        	// ex.printStackTrace();
        }
        
        // create user in camunda if the user does not exist or update the existing user
        User newUser = processEngine.getIdentityService().createUserQuery().userId(user.getId()).singleResult();
        if (newUser == null) {
            newUser = processEngine.getIdentityService().newUser(user.getId());
            newUser.setPassword(java.util.UUID.randomUUID().toString());
            newUser.setFirstName(user.getFirstName());
            newUser.setLastName(user.getLastName());
            newUser.setEmail(user.getEmail());

            log.info(String.format("Created Keycloak User [%s]!", newUser.getId()));
        } else {
            newUser.setFirstName(user.getFirstName());
            newUser.setLastName(user.getLastName());
            newUser.setEmail(user.getEmail());

            log.info(String.format("Updated Keycloak User [%s]!", newUser.getId()));
        }

        // Save User to Camunda
        try {
            processEngine.getIdentityService().saveUser(newUser);
        } catch (Exception ex) {
        	ex.printStackTrace();
        	
        	log.error(String.format("Failed to save user [%s]! Error: %s", newUser.getId(), ex.getMessage()));
        }

        // Store User
        user.setCamundaUser(newUser);

        // check user's app authorizations by iterating of list of apps
        user.getAuthorizedApps().add("welcome");

        // Create client roles (`camunda-` and `tenant-`)
        for (String role : user.getRoles()) {
            // Create Groups
            if (!role.startsWith("tenant-")) {
                getHelper().createGroup(role);
            }

            // Create Tenants
            if (role.startsWith("tenant-")) {
                String tenantId = role.substring("tenant-".length());
                getHelper().createTenant(tenantId);

                // Store Tenants
                user.getTenants().add(tenantId);
            }

            // Application Auth
            if (role.equals("camunda-user")) {
                user.getAuthorizedApps().add("tasklist");
            }
            if (role.equals("camunda-operator")) {
                user.getAuthorizedApps().add("cockpit");
            }
            if (role.equals("camunda-admin")) {
                user.getAuthorizedApps().add("admin");
            }
        }
        
        // sync membership
        getHelper().syncGroupMembership(user);
        getHelper().syncTenantMembership(user);

        // grant all permissions to superadmin group
        getHelper().createDefaultPermissions();
    }

}
