package nl.lekkeratlas.shared.exceptions;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends HttpResponsableException {

        public UserNotFoundException(String message) {
                super(HttpStatus.NOT_FOUND, message);
        }
}
