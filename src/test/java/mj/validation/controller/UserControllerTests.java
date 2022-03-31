package mj.validation.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import mj.validation.domain.User;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest
public class UserControllerTests {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    public void nonExistEmailUser_post_fieldError() {
        User user = new User(1L, null, "123456");
        String content = asJsonString(user);
        mockMvc.perform(post("/user")
            .content(content)
            .)
    }
    
    private String asJsonString(Object obj) throws Exception {
        return new ObjectMapper().writeValueAsString(obj);
    }
}