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
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.MediaEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.MediaService;
import io.gravitee.rest.api.service.exceptions.UploadUnauthorized;
import io.swagger.annotations.Api;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.io.InputStream;

@Api(tags = {"Portal"})
public class PortalMediaResource extends AbstractResource {
    @Inject
    private MediaService mediaService;

    @POST
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.CREATE)
    })
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("text/plain")
    public Response upload(
            @FormDataParam("file") InputStream uploadedInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail,
            @FormDataParam("file") final FormDataBodyPart body
    ) throws IOException {
        String mediaId;
        checkImageFormat(body.getMediaType());
        if (fileDetail.getSize() > this.mediaService.getMediaMaxSize()) {
            throw new UploadUnauthorized("Max size achieved " + fileDetail.getSize());
        } else {
            mediaId = mediaService.savePortalMedia(new MediaEntity(
                    uploadedInputStream,
                    body.getMediaType().getType(),
                    body.getMediaType().getSubtype(),
                    fileDetail.getFileName(),
                    fileDetail.getSize()
            ));
        }

        return Response.status(200).entity(mediaId).build();
    }

    @GET
    @Path("/{hash}")
    public Response getImage(
            @Context Request request,
            @PathParam("hash") String hash) {

        MediaEntity mediaEntity = mediaService.findby(hash);

        if (mediaEntity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        CacheControl cc = new CacheControl();
        cc.setNoTransform(true);
        cc.setMustRevalidate(false);
        cc.setNoCache(false);
        cc.setMaxAge(86400);


        EntityTag etag = new EntityTag(hash);
        Response.ResponseBuilder builder = request.evaluatePreconditions(etag);

        if (builder != null) {
            // Preconditions are not met, returning HTTP 304 'not-modified'
            return builder
                    .cacheControl(cc)
                    .build();
        }

        return Response
                .ok(mediaEntity.getData())
                .type(mediaEntity.getMimeType())
                .cacheControl(cc)
                .tag(etag)
                .build();
    }
}
