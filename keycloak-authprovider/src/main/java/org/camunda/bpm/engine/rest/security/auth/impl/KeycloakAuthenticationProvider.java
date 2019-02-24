package org.camunda.bpm.engine.rest.security.auth.impl;

import org.camunda.bpm.common.security.keycloak.CamundaSSOKeycloak;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.rest.security.auth.AuthenticationProvider;
import org.camunda.bpm.engine.rest.security.auth.AuthenticationResult;
import org.camunda.bpm.sso.domain.SSOUser;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

/**
 * Authentication Filter to protect the camunda rest api with keycloak-based authentication
 */
public class KeycloakAuthenticationProvider implements AuthenticationProvider {

    protected static final String BASIC_AUTH_HEADER_PREFIX = "Basic ";

    @Override
    public AuthenticationResult extractAuthenticatedUser(HttpServletRequest request, ProcessEngine engine) {
        try {
            CamundaSSOKeycloak keycloak = new CamundaSSOKeycloak(request);
            keycloak.setProcessEngine(engine);

            Optional<SSOUser> ssoUser = keycloak.obtainUserInformation();

            // check if a user was found
            if (ssoUser.isPresent()) {
                keycloak.process(ssoUser.get());

                // only allow access, if the user has the camunda-api role
                if (ssoUser.get().getRoles().contains("camunda-api")) {
                    return AuthenticationResult.successful(ssoUser.get().getId());
                } else {
                    return AuthenticationResult.unsuccessful(ssoUser.get().getId());
                }
            } else {
                return AuthenticationResult.unsuccessful();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            return AuthenticationResult.unsuccessful();
        }
    }

    @Override
    public void augmentResponseByAuthenticationChallenge(HttpServletResponse response, ProcessEngine engine) {
        // response.setHeader("Authenticate", BASIC_AUTH_HEADER_PREFIX + "realm=\"" + engine.getName() + "\"");
    }
}
