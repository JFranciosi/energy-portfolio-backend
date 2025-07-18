package miesgroup.mies.webdev.Rest.Exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import miesgroup.mies.webdev.Service.exception.WrongUsernameOrPasswordException;

@Provider
public class WrongUsernameOrPasswordExceptionMapper implements ExceptionMapper<WrongUsernameOrPasswordException> {
    @Override
    public Response toResponse(WrongUsernameOrPasswordException exception){
        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(exception.getMessage())
                .build();
    }
}