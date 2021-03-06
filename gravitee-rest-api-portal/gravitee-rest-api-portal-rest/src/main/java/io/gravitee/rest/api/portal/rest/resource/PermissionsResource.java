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
package io.gravitee.rest.api.portal.rest.resource;


import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.permissions.ApiPermission;
import io.gravitee.rest.api.model.permissions.ApplicationPermission;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.*;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.DELETE;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PermissionsResource extends AbstractResource {

    @Inject
    private MembershipService membershipService;

    @Inject
    private ApiService apiService;

    @Inject
    private ApplicationService applicationService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCurrentUserPermissions(@QueryParam("apiId") String apiId, @QueryParam("applicationId") String applicationId) {
        final String userId = getAuthenticatedUser();
        if (apiId != null) {
            ApiQuery apiQuery = new ApiQuery();
            apiQuery.setIds(Collections.singletonList(apiId));
            Set<ApiEntity> publishedByUser = apiService.findPublishedByUser(getAuthenticatedUserOrNull(), apiQuery);
            ApiEntity apiEntity = publishedByUser.stream().filter(a -> a.getId().equals(apiId)).findFirst().orElseThrow(() -> new ApiNotFoundException(apiId));
            Map<String, char[]> permissions;
            if (isAdmin()) {
                permissions = new HashMap<>();
                final char[] rights = new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()};
                for (ApiPermission perm : ApiPermission.values()) {
                    permissions.put(perm.getName(), rights);
                }
            } else {
                permissions = membershipService.getMemberPermissions(apiEntity, userId);
            }

            return Response
                    .ok(permissions)
                    .build();

        } else if (applicationId != null) {

            ApplicationListItem applicationListItem = applicationService.findByUser(getAuthenticatedUser())
                    .stream().filter(a -> a.getId().equals(applicationId))
                    .findFirst().orElseThrow(() -> new ApplicationNotFoundException(applicationId));

            ApplicationEntity application = applicationService.findById(applicationListItem.getId());

            Map<String, char[]> permissions;
            if (isAdmin()) {
                permissions = new HashMap<>();
                final char[] rights = new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()};
                for (ApplicationPermission perm : ApplicationPermission.values()) {
                    permissions.put(perm.getName(), rights);
                }
            } else {
                permissions = membershipService.getMemberPermissions(application, userId);
            }

            return Response
                    .ok(permissions)
                    .build();
        }
        throw new BadRequestException("One of the two parameters appId or applicationId must not be null.");
    }

    protected boolean isAdmin() {
        return isUserInRole(PORTAL_ADMIN);
    }

    private boolean isUserInRole(String role) {
        return securityContext.isUserInRole(role);
    }

}
