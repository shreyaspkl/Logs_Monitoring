package com.example.logsapi.controller;

import com.example.logsapi.model.Project;
import com.example.logsapi.model.Role;
import com.example.logsapi.model.UserProjectRoleBinding;
import com.example.logsapi.repository.ProjectRepository;
import com.example.logsapi.repository.UserProjectRoleBindingRepository;
import com.example.logsapi.utility.JwtFilter;
import com.example.logsapi.utility.OAuth2LoginSuccessHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProjectRepository projectRepository;
    @MockBean
    private UserProjectRoleBindingRepository bindingRepository;
    @MockBean
    private JwtFilter jwtFilter;
    @MockBean
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Test
    @WithMockUser(username = "user1")
    void requiredRoleViewerReturnsSupersetOfAdminAndHasExpectedShape() throws Exception {
        Project project = new Project();
        project.setProjectKey("PAYMENTS");
        project.setName("Payments Service");
        Field idField = Project.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(project, 1L);

        Project project2 = new Project();
        project2.setProjectKey("ORDERS");
        project2.setName("Orders Service");
        idField.setAccessible(true);
        idField.set(project2, 2L);

        Role viewerRole = new Role();
        viewerRole.setName("VIEWER");
        Role adminRole = new Role();
        adminRole.setName("ADMIN");

        UserProjectRoleBinding b1 = new UserProjectRoleBinding();
        b1.setProject(project);
        b1.setRole(viewerRole);

        UserProjectRoleBinding b2 = new UserProjectRoleBinding();
        b2.setProject(project2);
        b2.setRole(adminRole);

        when(bindingRepository.findByUserUsername("user1")).thenReturn(List.of(b1, b2));
        when(projectRepository.findAllById(org.mockito.ArgumentMatchers.anySet()))
                .thenReturn(List.of(project, project2));

        mockMvc.perform(get("/api/projects").param("requiredRole", "VIEWER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].projectKey").value("PAYMENTS"))
                .andExpect(jsonPath("$[0].name").value("Payments Service"))
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/projects").param("requiredRole", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(username = "user2")
    void userWithNoBindingsGetsEmptyArray() throws Exception {
        when(bindingRepository.findByUserUsername("user2")).thenReturn(List.of());

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(username = "user1")
    void invalidRequiredRoleReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/projects").param("requiredRole", "OWNER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("requiredRole must be one of VIEWER, OPERATOR, ADMIN"));
    }
}
