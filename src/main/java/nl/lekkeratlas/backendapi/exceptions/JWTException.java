package nl.lekkeratlas.backendapi.exceptions;

import org.springframework.http.HttpStatus;

import nl.lekkeratlas.shared.exceptions.HttpResponsableException;

public class JWTException extends HttpResponsableException {

        public JWTException(HttpStatus status, String message) {
                super(status, message);
        }
}
