package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.model.User;
import com.shepherdmoney.interviewproject.repository.UserRepository;
import com.shepherdmoney.interviewproject.vo.request.CreateUserPayload;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

@Autowired
    private UserRepository userRepository;
    @PutMapping("/user")
    public ResponseEntity<Integer> createUser(@RequestBody CreateUserPayload payload) {
        Optional<User> existingUser = userRepository.findByEmail(payload.getEmail()); //find user by email
        if (existingUser.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(-1);
        }
        //Create a new user if it doesn't exist
        User newUser = new User(); 
        newUser.setName(payload.getName());
        newUser.setEmail(payload.getEmail());
        newUser = userRepository.save(newUser);  
        return ResponseEntity.ok(newUser.getId());  
    }

    @DeleteMapping("/user")
    public ResponseEntity<String> deleteUser(@RequestParam int userId) {
        Optional<User> user = userRepository.findById(userId); 
        if(user.isPresent()) {
            userRepository.delete(user.get()); 
            return ResponseEntity.ok("User deleted successfully");
        } else {
            return ResponseEntity.badRequest().body("User not found"); 
        }
    }
}
