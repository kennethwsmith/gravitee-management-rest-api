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
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.rest.api.model.permissions.*;
import io.swagger.annotations.Api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Roles"})
public class RoleScopesResource extends AbstractResource  {

    @Context
    private ResourceContext resourceContext;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<String>> list()  {
        final Map<String, List<String>> roles = new LinkedHashMap<>(4);
        roles.put(RoleScope.ORGANIZATION.name(), stream(OrganizationPermission.values()).map(OrganizationPermission::getName).sorted().collect(toList()));
        roles.put(RoleScope.ENVIRONMENT.name(), stream(EnvironmentPermission.values()).map(EnvironmentPermission::getName).sorted().collect(toList()));
        roles.put(RoleScope.API.name(), stream(ApiPermission.values()).map(ApiPermission::getName).sorted().collect(toList()));
        roles.put(RoleScope.APPLICATION.name(), stream(ApplicationPermission.values()).map(ApplicationPermission::getName).sorted().collect(toList()));
        return roles;
    }

    @Path("{scope}/roles")
    public RoleScopeResource getRoleScopeResource() {
        return resourceContext.getResource(RoleScopeResource.class);
    }
}
