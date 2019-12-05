package net.trystram.scaletest.infinispan;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.jboss.resteasy.annotations.jaxrs.PathParam;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/rest/v2")
@RegisterRestClient
public interface Infinispan {

    @GET
    @Path("/caches/{name}?action=size")
    @Produces("text/plain")
    public long cacheSize(@PathParam String name);

}
