package mj.validation.controller;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import mj.validation.controller.result.Success;
import mj.validation.domain.User;

@RestController
public class UserController {

    @PostMapping("/user")
    public ResponseEntity<Success> newUser(@Valid @RequestBody User user) {
        return ResponseEntity.ok(new Success("성공"));
    } 
}