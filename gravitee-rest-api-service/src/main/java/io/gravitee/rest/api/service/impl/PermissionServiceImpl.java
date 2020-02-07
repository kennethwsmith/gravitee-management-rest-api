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
package io.gravitee.rest.api.service.impl;

import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Nicolas GERAUD(nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class PermissionServiceImpl extends AbstractService implements PermissionService {

    @Autowired
    MembershipService membershipService;

    @Autowired
    RoleService roleService;

    @Override
    public boolean hasPermission(RolePermission permission, String referenceId, RolePermissionAction... acls) {
        MembershipReferenceType membershipReferenceType;
        switch (permission.getScope()) {
            case API:
                membershipReferenceType = MembershipReferenceType.API;
                break;
            case APPLICATION:
                membershipReferenceType = MembershipReferenceType.APPLICATION;
                break;
            case ENVIRONMENT:
                membershipReferenceType = MembershipReferenceType.ENVIRONMENT;
                break;
            default:
                membershipReferenceType = null;
        }
        
        MemberEntity member = membershipService.getUserMember(membershipReferenceType, referenceId, getAuthenticatedUsername());
        if (member == null ) {
            return false;
        }
        return roleService.hasPermission(member.getPermissions(), permission.getPermission(), acls);
    }
}
