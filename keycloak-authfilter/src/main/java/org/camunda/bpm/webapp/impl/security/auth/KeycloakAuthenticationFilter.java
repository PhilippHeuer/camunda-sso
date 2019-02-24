package org.camunda.bpm.webapp.impl.security.auth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.camunda.bpm.common.security.keycloak.CamundaSSOKeycloak;
import org.camunda.bpm.sso.CamundaSSOCore;
import org.camunda.bpm.sso.domain.SSOUser;
import org.camunda.bpm.webapp.impl.security.SecurityActions;
import org.camunda.bpm.webapp.impl.security.SecurityActions.SecurityAction;

/**
 * A AuthenticationFilter the protect the camunda webapps with keycloak-based authentication
 */
public class KeycloakAuthenticationFilter implements Filter {

    public void init(FilterConfig filterConfig) throws ServletException {

    }

    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {

        final HttpServletRequest req = (HttpServletRequest) request;

        // get authentication from session
        Authentications authentications = Authentications.getFromSession(req.getSession());

        // Manipulate the default AuthenticationFilter to handle SSO
        setSSOAuthentication(request, authentications);

        // set current authentication
        Authentications.setCurrent(authentications);

        try {
            SecurityActions.runWithAuthentications(new SecurityAction<Void>() {
                public Void execute() {
                    try {
                        chain.doFilter(request, response);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    return null;
                }
            }, authentications);
        } finally {
            Authentications.clearCurrent();
            Authentications.updateSession(req.getSession(), authentications);
        }
    }

    protected void clearProcessEngineAuthentications(Authentications authentications) {

    }

    public void destroy() {

    }

    /**
     * Keycloak-specific workflow to handle authentication
     *
     * @param request Request
     * @param authentications Camunda Authentications
     */
    protected void setSSOAuthentication(final ServletRequest request, Authentications authentications) {
        CamundaSSOCore keycloak = new CamundaSSOKeycloak(request);

        // if already in the list of logged in users - nothing to do (only for frontend / authentications.isPresent)
        Authentication authentication = authentications.getAuthenticationForProcessEngine(keycloak.getProcessEngine().getName());
        Optional<SSOUser> ssoUser = keycloak.obtainUserInformation();
        if (authentication != null && ssoUser.isPresent() && authentication.getName().equals(ssoUser.get().getId())) {
            return;
        }
        
        // Process Token Information
        keycloak.process(ssoUser.get());
        
        // create new authentication object to store authentication
        List<String> groupIds = new ArrayList<String>();
        groupIds.addAll(ssoUser.get().getRoles());
        List<String> tenantIds = new ArrayList<String>();
        tenantIds.addAll(ssoUser.get().getTenants());
        UserAuthentication newAuthentication = new UserAuthentication(ssoUser.get().getId(), keycloak.getProcessEngine().getName());
        newAuthentication.setGroupIds(groupIds);
        newAuthentication.setTenantIds(tenantIds);
        newAuthentication.setAuthorizedApps(ssoUser.get().getAuthorizedApps());

        // and add the new logged in user
        authentications.addAuthentication(newAuthentication);
    }

}
