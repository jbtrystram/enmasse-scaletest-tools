package net.trystram.scaletest.infinispan;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/rest/v2")
@RegisterRestClient
public interface Infinispan {

    @GET
    @Path("/caches/{name}?action=size")
    public String cacheSize(@HeaderParam("Authorization") String authHeader, @PathParam String name);
}
