# Camunda SSO for WebApps / REST-API

## Description

This project supports the following SSO Services for Authentication in the WebApps and for the REST-API:

- Keycloak

## Features

This project will create/maintain all tenants, users and groups on the fly.

It supports the following use-cases:

 - Authentication Filter (Frontend Apps)
 - Authentication Provider (Rest Engine)

## Global Roles

There are the following roles, which allow basic access to the respective WebApps:

- camunda-user (Tasklist)
- camunda-api (REST API)
- camunda-operator (Operator Dashboard)
- camunda-admin (Admin)

## Project Modules

| Module | Description |
|---|---|
| common | common sso relevant code |
| keycloak | keycloak implementation of sso features |
| keycloak-authfilter | auth filter using keycloak |
| keycloak-authprovider | auth provider using keycloak |

## Installation

### Docker

I will provide a docker image where keycloak sso is already installed, so that you just have to insert the config into the container.

Link will be added here later.

### Tomcat

#### Keycloak Adapter

You need to install the keycloak adapter first, download the package (OpenID Connnect - Tomcat 8) from this url: http://www.keycloak.org/downloads.html
Extract it your your tomcat's `lib` dir, in the docker container it's `/camunda/lib`.

Additionally you need to place the jars for the `keycloak-servlet-filter-adapter` and `keycloak-servlet-adapter-spi` into your tomcat /lib folder.

#### WebApps (Tasklist, Cockpit, Admin)

*webapps/camunda/lib*

Place the following jars into the library folder:

- camunda-sso-common
- camunda-sso-keycloak
- camunda-sso-keycloak-authfilter

You can download them here: https://dl.bintray.com/philippheuer/maven/com/github/philippheuer/camunda/sso/ or directly using jcenter.

*webapps/camunda/WEB-INF/web.xml*

Add the following section above the SecurityFilter section:

```xml
<!-- KeyCloak OpenID Connect Filter -->
  <filter>
    <filter-name>KeyCloak OpenID Connect Filter</filter-name>
    <filter-class>org.keycloak.adapters.servlet.KeycloakOIDCFilter</filter-class>
    <init-param>
	    <param-name>keycloak.config.file</param-name>
	    <param-value>/app/conf/keycloak.json</param-value>
    </init-param>
  </filter>
  <filter-mapping>
    <filter-name>KeyCloak OpenID Connect Filter</filter-name>
    <url-pattern>/*</url-pattern>
    <dispatcher>REQUEST</dispatcher>
  </filter-mapping>
  
  <!-- Keycloak Authentication Filter -->
  <filter>
    <filter-name>Authentication Filter</filter-name>
	<filter-class>org.camunda.community.sso.keycloak.KeycloakAuthenticationFilter</filter-class>
  </filter>
  <filter-mapping>
    <filter-name>Authentication Filter</filter-name>
    <url-pattern>/*</url-pattern>
    <dispatcher>REQUEST</dispatcher>
  </filter-mapping>
```

Make sure to replace the `keycloak.config.file` with the path to your config file.

#### Engine-REST (API)

*webapps/engine-rest/lib*

Place the following jars into the library folder:

- camunda-sso-common
- camunda-sso-keycloak
- camunda-sso-keycloak-authprovider

You can download them here: https://dl.bintray.com/philippheuer/maven/com/github/philippheuer/camunda/sso/ or directly using jcenter.

`engine-rest/lib`

*webapps/engine-rest/WEB-INF/web.xml*

Add the following section above the RestEasy section:

```xml
<!-- KeyCloak OpenID Connect Filter -->
  <filter>
    <filter-name>KeyCloak OpenID Connect Filter</filter-name>
    <filter-class>org.keycloak.adapters.servlet.KeycloakOIDCFilter</filter-class>
    <init-param>
	    <param-name>keycloak.config.file</param-name>
	    <param-value>/app/conf/keycloak.json</param-value>
    </init-param>
  </filter>
  <filter-mapping>
    <filter-name>KeyCloak OpenID Connect Filter</filter-name>
    <url-pattern>/*</url-pattern>
    <dispatcher>REQUEST</dispatcher>
  </filter-mapping>

  <!-- Keycloak Authentication Filter -->
  <filter>
    <filter-name>camunda-auth</filter-name>
    <filter-class>org.camunda.bpm.engine.rest.security.auth.ProcessEngineAuthenticationFilter</filter-class>
    <init-param>
      <param-name>authentication-provider</param-name>
      <param-value>org.camunda.community.sso.keycloak.KeycloakAuthenticationProvider</param-value>
    </init-param>
    <init-param>
		<param-name>rest-url-pattern-prefix</param-name>
		<param-value></param-value>
	</init-param>
  </filter>
  <filter-mapping>
    <filter-name>camunda-auth</filter-name>
    <url-pattern>/*</url-pattern>
  </filter-mapping>
  <!-- /End Keycloak Authentication Filter -->
```

Make sure to replace the `keycloak.config.file` with the path to your config file.
