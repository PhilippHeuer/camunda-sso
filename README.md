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

#### WebApps (Tasklist, Cockpit, Admin)

*Added later*

#### Engine-REST (API)

*Added later*
