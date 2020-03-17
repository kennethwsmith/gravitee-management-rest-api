/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.rest.resource.auth;

import com.auth0.jwt.JWTSigner;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.security.cookies.JWTCookieGenerator;
import io.gravitee.rest.api.management.rest.model.TokenEntity;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.JWTHelper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotBlank;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static io.gravitee.rest.api.management.rest.model.TokenType.BEARER;
import static io.gravitee.rest.api.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_EXPIRE_AFTER;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
abstract class AbstractAuthenticationResource {

    @Autowired
    protected Environment environment;
    @Autowired
    protected UserService userService;
    @Autowired
    protected MembershipService membershipService;
    @Autowired
    protected JWTCookieGenerator jwtCookieGenerator;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static final String CLIENT_ID_KEY = "client_id", REDIRECT_URI_KEY = "redirect_uri",
            CLIENT_SECRET = "client_secret", CODE_KEY = "code", GRANT_TYPE_KEY = "grant_type",
            AUTH_CODE = "authorization_code", TOKEN = "token";

    protected Map<String, Object> getResponseEntity(final Response response) throws IOException {
        return getEntity((getResponseEntityAsString(response)));
    }

    protected String getResponseEntityAsString(final Response response) throws IOException {
        return response.readEntity(String.class);
    }

    protected Map<String, Object> getEntity(final String response) throws IOException {
        return MAPPER.readValue(response, new TypeReference<Map<String, Object>>() {});
    }

    protected Response connectUser(String userId, final HttpServletResponse servletResponse) {
        UserEntity user = userService.connect(userId);

        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        final UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // Manage authorities, initialize it with dynamic permissions from the IDP
        Set<GrantedAuthority> authorities = new HashSet<>(userDetails.getAuthorities());

        // We must also load permissions from repository for configured management or portal role
        Set<RoleEntity> userRoles = membershipService.getRoles(
                MembershipReferenceType.ENVIRONMENT,
                GraviteeContext.getCurrentEnvironment(),
                MembershipMemberType.USER,
                userDetails.getId());
        if (!userRoles.isEmpty()) {
            userRoles.forEach(role -> authorities.add(new SimpleGrantedAuthority(role.getScope().toString() + ':' + role.getName())));
        }
        userRoles = membershipService.getRoles(
                MembershipReferenceType.ORGANIZATION,
                GraviteeContext.getCurrentOrganization(),
                MembershipMemberType.USER,
                userDetails.getId());
        if (!userRoles.isEmpty()) {
            userRoles.forEach(role -> authorities.add(new SimpleGrantedAuthority(role.getScope().toString() + ':' + role.getName())));
        }

        // JWT signer
        final Map<String, Object> claims = new HashMap<>();

        claims.put(JWTHelper.Claims.ISSUER, environment.getProperty("jwt.issuer", JWTHelper.DefaultValues.DEFAULT_JWT_ISSUER));
        claims.put(JWTHelper.Claims.SUBJECT, user.getId());
        claims.put(JWTHelper.Claims.PERMISSIONS, authorities);
        claims.put(JWTHelper.Claims.EMAIL, user.getEmail());
        claims.put(JWTHelper.Claims.FIRSTNAME, user.getFirstname());
        claims.put(JWTHelper.Claims.LASTNAME, user.getLastname());

        final JWTSigner.Options options = new JWTSigner.Options();
        options.setExpirySeconds(environment.getProperty("jwt.expire-after", Integer.class, DEFAULT_JWT_EXPIRE_AFTER));
        options.setIssuedAt(true);
        options.setJwtId(true);

        final String sign = new JWTSigner(environment.getProperty("jwt.secret")).sign(claims, options);
        final TokenEntity tokenEntity = new TokenEntity();
        tokenEntity.setType(BEARER);
        tokenEntity.setToken(sign);

        final Cookie bearerCookie = jwtCookieGenerator.generate("Bearer%20" + sign);
        servletResponse.addCookie(bearerCookie);

        return Response
                .ok(tokenEntity)
                .build();
    }

    public static class Payload {
        @NotBlank
        String clientId;

        @NotBlank
        String redirectUri;

        @NotBlank
        String code;

        String state;

        public String getClientId() {
            return clientId;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public String getCode() {
            return code;
        }

        public String getState() {
            return state;
        }
    }
}
