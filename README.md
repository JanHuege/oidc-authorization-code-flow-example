# Disclaimer
This is a simple playground project. I do not recommend implementing OIDC-AuthCode-Flow like that. Use appropriate libraries instead!
# How to use
- `docker run -p 8090:8080 -e KEYCLOAK_USER=admin -e KEYCLOAK_PASSWORD=admin jboss/keycloak`
- Import `realm-export.json`
- add a user to `jee` realm
- download any Java EE 8 compliant AppServer (https://wildfly.org/downloads/)
- Deploy `.war`