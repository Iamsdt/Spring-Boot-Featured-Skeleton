package com.firti.webservice.controllers.api.firebase;

import com.firti.webservice.entities.User;
import com.firti.webservice.entities.annotations.CurrentUser;
import com.firti.webservice.exceptions.invalid.InvalidException;
import com.firti.webservice.exceptions.notfound.UserNotFoundException;
import com.firti.webservice.services.FirebaseTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@RequestMapping("/api/v1/firebase")
public class FirebaseController {
    private final FirebaseTokenService firebaseTokenService;

    @Autowired
    public FirebaseController(FirebaseTokenService firebaseTokenService) {
        this.firebaseTokenService = firebaseTokenService;
    }

    @PostMapping("/token")
    @ResponseStatus(HttpStatus.OK)
    private void saveToken(@CurrentUser User user,
                           @RequestParam("token") String token) throws UserNotFoundException, InvalidException {
        this.firebaseTokenService.save(user.getId(), token);
    }

}
