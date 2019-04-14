package org.camunda.community.sso;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.BpmPlatform;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.community.sso.domain.SSOUser;

import javax.servlet.ServletRequest;
import java.util.Optional;

/**
 * CamundaSSOCore
 * <p>
 * This class contains core features needed by every camunda sso provider
 */
@Data
@Slf4j
public abstract class CamundaSSOCore {

    /**
     * The Servlet Request
     */
    private ServletRequest request;

    /**
     * Camunda SSO Helper
     */
    protected CamundaSSOHelper helper;

    /**
     * The SSO User
     */
    protected SSOUser user;

    /**
     * The Camunda Process Engine
     */
    @Getter
    @Setter
    protected ProcessEngine processEngine;

    /**
     * Gets the ProcessEngine
     * <p>
     * Only works in single engine environment!
     *
     * @return ProcessEngine The Default ProcessEngine
     */
    protected ProcessEngine getDefaultProcessEngine() {
        return BpmPlatform.getDefaultProcessEngine();
    }

    /**
     * Constructor
     *
     * @param request ServletRequest
     */
    public CamundaSSOCore(ServletRequest request) {
        // Initialize
        this.request = request;
        this.processEngine = getDefaultProcessEngine();
        this.helper = new CamundaSSOHelper(request, this.processEngine);
    }

    /**
     * Gets User Information from the request
     *
     * @return Username
     */
    public abstract Optional<SSOUser> obtainUserInformation();

    /**
     * Process Request
     */
    public abstract void process(SSOUser user);

}
