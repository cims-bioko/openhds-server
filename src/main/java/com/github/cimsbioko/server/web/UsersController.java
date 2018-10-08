package com.github.cimsbioko.server.web;

import com.github.cimsbioko.server.dao.RoleRepository;
import com.github.cimsbioko.server.dao.UserRepository;
import com.github.cimsbioko.server.domain.User;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Controller
public class UsersController {

    private UserRepository userRepo;
    private RoleRepository roleRepo;
    private MessageSource messages;

    public UsersController(UserRepository userRepo, RoleRepository roleRepo, MessageSource messages) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.messages = messages;
    }

    @GetMapping("/users")
    public ModelAndView users(@RequestParam(name = "p", defaultValue = "0") Integer page) {
        // FIXME: only show non-deleted
        ModelAndView modelAndView = new ModelAndView("users");
        modelAndView.addObject("users", userRepo.findByDeletedIsNull(new PageRequest(page, 10)));
        modelAndView.addObject("roles", roleRepo.findAll());
        return modelAndView;
    }

    @PostMapping("/users")
    @ResponseBody
    public ResponseEntity<AjaxResult> createUser(@Valid @RequestBody UserForm userForm, Locale locale) {

        if (userRepo.findByUsernameAndDeletedIsNull(userForm.getUsername()) != null) {
            return ResponseEntity
                    .badRequest()
                    .body(new AjaxResult()
                            .addError(resolveMessage("input.msg.errors", locale))
                            .addFieldError("username",
                                    resolveMessage("users.msg.exists", locale, userForm.getUsername())));
        }

        User u = new User();
        u.setFirstName(userForm.getFirstName());
        u.setLastName(userForm.getLastName());
        u.setDescription(userForm.getDescription());
        u.setUsername(userForm.getUsername());
        u.setPassword(userForm.getPassword());
        u.setRoles(roleRepo.findByUuidIn(Arrays.stream(userForm.getRoles()).collect(Collectors.toSet())));
        u = userRepo.save(u);

        return ResponseEntity
                .ok(new AjaxResult()
                        .addMessage(
                                resolveMessage("users.msg.created", locale, u.getUsername())));
    }

    @ExceptionHandler
    @ResponseStatus(BAD_REQUEST)
    @ResponseBody
    public AjaxResult handleInvalidArgument(MethodArgumentNotValidException e, Locale locale) {
        AjaxResult result = new AjaxResult();
        BindingResult bindResult = e.getBindingResult();
        result.addError(resolveMessage("input.msg.errors", locale));
        bindResult.getGlobalErrors()
                .stream()
                .map(oe -> messages.getMessage(oe, locale))
                .forEach(result::addError);
        bindResult.getFieldErrors()
                .forEach(fe -> result.addFieldError(fe.getField(), messages.getMessage(fe, locale)));
        return result;
    }

    private String resolveMessage(String key, Locale locale, Object... args) {
        return messages.getMessage(key, args, locale);
    }


    static class UserForm {

        private String firstName;
        private String lastName;
        private String description;
        @NotNull
        @Size(max = 255)
        private String username;
        @NotNull
        @Size(min = 8, max = 255)
        private String password;
        @NotNull
        private String[] roles;

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        @NotNull
        public String getUsername() {
            return username;
        }

        public void setUsername(@NotNull String username) {
            this.username = username;
        }

        @NotNull
        public String getPassword() {
            return password;
        }

        public void setPassword(@NotNull String password) {
            this.password = password;
        }

        @NotNull
        public String[] getRoles() {
            return roles;
        }

        public void setRoles(@NotNull String[] roles) {
            this.roles = roles;
        }
    }
}
