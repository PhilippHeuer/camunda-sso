package org.camunda.community.sso;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.authorization.Resource;
import org.camunda.bpm.engine.authorization.Resources;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.Tenant;
import org.camunda.bpm.engine.impl.persistence.entity.AuthorizationEntity;
import org.camunda.bpm.engine.impl.persistence.entity.TenantEntity;
import org.camunda.community.sso.domain.SSOUser;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import java.util.List;

import static org.camunda.bpm.engine.authorization.Authorization.ANY;
import static org.camunda.bpm.engine.authorization.Authorization.AUTH_TYPE_GRANT;
import static org.camunda.bpm.engine.authorization.Permissions.ALL;

@Getter
@Slf4j
public class CamundaSSOHelper {

    /**
     * The Servlet Request
     */
    private ServletRequest request;

    /**
     * The HTTP Servlet Request
     */
    private HttpServletRequest req;

    /**
     * The Camunda Process Engine
     */
    private ProcessEngine processEngine;

    /**
     * Constructor
     *
     * @param request ServletRequest
     */
    public CamundaSSOHelper(ServletRequest request, ProcessEngine processEngine) {
        // Initialize
        this.request = request;
        this.req = (HttpServletRequest) request;
        this.processEngine = processEngine;
    }

    /**
     * Creates the default permission groups, if they aren't existing yet
     */
    public void createDefaultPermissions() {
        for (Resource resource : Resources.values()) {
            String adminGroup = "camunda-admin";
            if (processEngine.getAuthorizationService().createAuthorizationQuery().groupIdIn(adminGroup).resourceType(resource).resourceId(ANY).count() == 0) {
                AuthorizationEntity adminGroupAuth = new AuthorizationEntity(AUTH_TYPE_GRANT);
                adminGroupAuth.setGroupId(adminGroup);
                adminGroupAuth.setResource(resource);
                adminGroupAuth.setResourceId(ANY);
                adminGroupAuth.addPermission(ALL);
                processEngine.getAuthorizationService().saveAuthorization(adminGroupAuth);

                log.info(String.format("Permissions to Resource %s granted for group `camunda-admin`!", resource.resourceName()));
            }
        }
    }

    /**
     * Creates a Group, if it's not existing yet
     */
    public void createGroup(String groupId) {
        if (groupId.length() > 64) {
            log.error(String.format("Failed to create group %s, max length of 64 exceeded!", groupId));
            return;
        }

        Group group = processEngine.getIdentityService().createGroupQuery().groupId(groupId).singleResult();
        if (group == null) {
            group = processEngine.getIdentityService().newGroup(groupId);
            group.setName(groupId);

            if (groupId.startsWith("camunda-")) {
                group.setType("SYSTEM");
            } else {
                group.setType("WORKFLOW");
            }
            processEngine.getIdentityService().saveGroup(group);

            log.info(String.format("Added client group: [%s]!", group.getName()));
        }
    }

    /**
     * Creates a Tenant, if it's not existing yet
     */
    public void createTenant(String tenantId) {
        if (tenantId.length() > 64) {
            log.error(String.format("Failed to create tenant %s, max length of 64 exceeded!", tenantId));
            return;
        }

        Tenant tenant = processEngine.getIdentityService().createTenantQuery().tenantId(tenantId).singleResult();
        if (tenant == null) {
            tenant = new TenantEntity();
            tenant.setId(tenantId);
            tenant.setName(tenantId);

            processEngine.getIdentityService().saveTenant(tenant);
            log.info(String.format("Added tenant: [%s]!", tenant.getName()));
        }
    }

    /**
     * Syncs the group memerships for the current user
     */
    public void syncGroupMembership(SSOUser user) {
        List<Group> allGroups = processEngine.getIdentityService().createGroupQuery().list();
        for (Group group : allGroups) {
            // check if the user should have the current group
            if (user.getRoles().contains(group.getId())) {
                // check if the user already is a member
                if (processEngine.getIdentityService().createUserQuery().userId(user.getCamundaUser().getId()).memberOfGroup(group.getId()).count() == 0) {
                    // assign group, since the user doesn't have it
                    processEngine.getIdentityService().createMembership(user.getCamundaUser().getId(), group.getId());
                    log.info(String.format("Added user [%s] to group [%s]!", user.getCamundaUser().getId(), group.getName()));
                }
            } else {
                processEngine.getIdentityService().deleteMembership(user.getCamundaUser().getId(), group.getId());

                // Remove groups without members
                if (processEngine.getIdentityService().createUserQuery().memberOfGroup(group.getId()).count() == 0) {
                    // Prevent removal of default admin group
                    if (!group.getId().equals("camunda-admin")) {
                        processEngine.getIdentityService().deleteGroup(group.getId());

                        log.info(String.format("Removed empty group [%s]!", group.getId()));
                    }
                }
            }
        }
    }

    /**
     * Syncs the tenants for the current user
     */
    public void syncTenantMembership(SSOUser user) {
        List<Tenant> allTenants = processEngine.getIdentityService().createTenantQuery().list();
        for (Tenant tenant : allTenants) {
            // check if the user should have the current group
            if (user.getRoles().contains("tenant-" + tenant.getId())) {
                // check if the user should have the tenant
                if (processEngine.getIdentityService().createUserQuery().userId(user.getCamundaUser().getId()).memberOfTenant(tenant.getId()).count() == 0) {
                    // assign tenant, since the user doesn't have it
                    processEngine.getIdentityService().createTenantUserMembership(tenant.getId(), user.getCamundaUser().getId());

                    log.info(String.format("Added tenant-membership for [%s] to user [%s]!", tenant.getId(), user.getCamundaUser().getId()));
                }
            } else {
                processEngine.getIdentityService().deleteTenantUserMembership(tenant.getId(), user.getCamundaUser().getId());

                // Remove tenants without members
                if (processEngine.getIdentityService().createUserQuery().memberOfTenant(tenant.getId()).count() == 0) {
                    processEngine.getIdentityService().deleteTenant(tenant.getId());

                    log.info(String.format("Removed tenant without members [%s]!", tenant.getId()));
                }
            }
        }
    }

}
