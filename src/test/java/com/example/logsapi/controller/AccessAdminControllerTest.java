package com.example.logsapi.controller;

import com.example.logsapi.model.Project;
import com.example.logsapi.model.Role;
import com.example.logsapi.model.User;
import com.example.logsapi.repository.ProjectRepository;
import com.example.logsapi.repository.RoleRepository;
import com.example.logsapi.repository.UserProjectRoleBindingRepository;
import com.example.logsapi.repository.UserRepository;
import com.example.logsapi.utility.AccessAdminService;
import com.example.logsapi.utility.JwtFilter;
import com.example.logsapi.utility.OAuth2LoginSuccessHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccessAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AccessAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;
    @MockBean
    private ProjectRepository projectRepository;
    @MockBean
    private RoleRepository roleRepository;
    @MockBean
    private UserProjectRoleBindingRepository bindingRepository;
    @MockBean
    private AccessAdminService accessAdminService;
    @MockBean
    private JwtFilter jwtFilter;
    @MockBean
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Test
    @WithMockUser(username = "admin")
    void assignWithInvalidProjectIdFails() throws Exception {
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        String body = """
                {
                  "username":"user1",
                  "projectId":999,
                  "environment":"DEV",
                  "roleName":"VIEWER"
                }
                """;

        mockMvc.perform(post("/api/access/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("projectId is required and must reference an existing project"));
    }

    @Test
    @WithMockUser(username = "admin")
    void assignWithValidProjectIdSucceedsWhenAuthorized() throws Exception {
        Project project = new Project();
        project.setProjectKey("PAYMENTS");
        project.setName("Payments Service");
        Field projectIdField = Project.class.getDeclaredField("id");
        projectIdField.setAccessible(true);
        projectIdField.set(project, 1L);

        User user = User.builder()
                .username("user1")
                .email("user1@example.com")
                .password("dummy")
                .build();

        Role role = new Role();
        Field roleIdField = Role.class.getDeclaredField("id");
        roleIdField.setAccessible(true);
        roleIdField.set(role, 10L);
        role.setName("VIEWER");

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(roleRepository.findByName("VIEWER")).thenReturn(Optional.of(role));
        when(bindingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String body = """
                {
                  "username":"user1",
                  "projectId":1,
                  "environment":"DEV",
                  "roleName":"VIEWER"
                }
                """;

        mockMvc.perform(post("/api/access/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("assigned"));
    }
}
