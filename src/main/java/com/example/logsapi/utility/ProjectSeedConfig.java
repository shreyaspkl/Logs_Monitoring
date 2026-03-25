package com.example.logsapi.utility;

import com.example.logsapi.model.Project;
import com.example.logsapi.repository.ProjectRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProjectSeedConfig {

    @Bean
    public CommandLineRunner seedProjects(ProjectRepository projectRepository) {
        return args -> {
            seed(projectRepository, "PAYMENTS", "Payments Service");
            seed(projectRepository, "ORDERS", "Orders Service");
            seed(projectRepository, "AUTH", "Auth Service");
        };
    }

    private void seed(ProjectRepository projectRepository, String projectKey, String name) {
        if (projectRepository.findByProjectKey(projectKey).isPresent()) {
            return;
        }
        Project project = new Project();
        project.setProjectKey(projectKey);
        project.setName(name);
        project.setActive(true);
        projectRepository.save(project);
    }
}
