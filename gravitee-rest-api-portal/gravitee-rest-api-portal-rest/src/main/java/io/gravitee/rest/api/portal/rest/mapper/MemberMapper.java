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
package io.gravitee.rest.api.portal.rest.mapper;

import java.time.ZoneOffset;

import org.springframework.stereotype.Component;

import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.portal.rest.model.Member;
import io.gravitee.rest.api.portal.rest.model.User;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MemberMapper {
    
    public Member convert(MemberEntity member) {
        final Member memberItem = new Member();
        
        memberItem.setCreatedAt(member.getCreatedAt().toInstant().atOffset(ZoneOffset.UTC));
        
        User memberUser = new User();
        memberUser.setId(member.getId());
        memberUser.setEmail(member.getEmail());
        memberUser.setDisplayName(member.getDisplayName());
        memberItem.setUser(memberUser);
        if(member.getRoles() != null && !member.getRoles().isEmpty()) {
            memberItem.setRole(member.getRoles().get(0).getName());
        }
        memberItem.setUpdatedAt(member.getUpdatedAt().toInstant().atOffset(ZoneOffset.UTC));
        
        return memberItem;
    }

}
