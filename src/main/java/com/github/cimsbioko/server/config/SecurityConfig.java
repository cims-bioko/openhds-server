package com.github.cimsbioko.server.config;

import com.github.cimsbioko.server.dao.UserRepository;
import com.github.cimsbioko.server.security.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.www.DigestAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.DigestAuthenticationFilter;

import static org.springframework.http.HttpMethod.*;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public RoleMapper roleMapper() {
        return new RoleMapper();
    }

    @Bean
    public UserMapper userMapper() {
        return new UserMapper(roleMapper());
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepo) {
        UserDetailsService service = new UserDetailsService();
        service.setUserMapper(userMapper());
        service.setUserRepository(userRepo);
        return service;
    }

    @Bean
    RunAsUserAspect runAsUserAspect(UserDetailsService userDetailsService) {
        return new RunAsUserAspect(userDetailsService);
    }

    @Bean
    LastLoginHandler lastLoginHandler(UserRepository userRepo) {
        return new LastLoginHandler(userRepo);
    }

    @Bean
    public DigestAuthenticationEntryPoint digestEntryPoint() {
        DigestAuthenticationEntryPoint entryPoint = new DigestAuthenticationEntryPoint();
        entryPoint.setRealmName("CIMS API");
        entryPoint.setKey("cims-dig-it");
        entryPoint.setNonceValiditySeconds(10);
        return entryPoint;
    }

    @Bean
    public DigestAuthenticationFilter digestAuthFilter(DigestAuthenticationEntryPoint entryPoint,
                                                       UserDetailsService userDetailsService) {
        DigestAuthenticationFilter filter = new DigestAuthenticationFilter();
        filter.setUserDetailsService(userDetailsService);
        filter.setAuthenticationEntryPoint(entryPoint);
        return filter;
    }

    @Configuration
    @Order(1)
    public static class ApiWebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

        protected void configure(HttpSecurity http) throws Exception {
            http.antMatcher("/api/**")
                    .headers().disable()
                    .csrf().disable()
                    .exceptionHandling()
                    .and()
                    .sessionManagement()
                    .sessionCreationPolicy(STATELESS)
                    .and()
                    .authorizeRequests()
                    .antMatchers("/api/rest/mobiledb").hasAuthority("MOBILE_SYNC")
                    .antMatchers(POST, "/api/odk/formUpload").hasAuthority("ODK_FORM_UPLOAD")
                    .antMatchers(POST, "/api/odk/forms").hasAuthority("ODK_FORM_UPLOAD")
                    .antMatchers(GET, "/api/odk/forms").hasAuthority("ODK_FORM_LIST")
                    .antMatchers(GET, "/api/odk/formList").hasAuthority("ODK_FORM_LIST")
                    .antMatchers(GET, "/api/odk/forms/**").hasAuthority("ODK_FORM_DOWNLOAD")
                    .antMatchers(GET, "/api/odk/formList/**").hasAuthority("ODK_FORM_DOWNLOAD")
                    .antMatchers(POST, "/api/odk/submission").hasAuthority("ODK_SUBMISSION_UPLOAD")
                    .antMatchers(GET, "/api/odk/view/submissionList").hasAuthority("ODK_SUBMISSION_LIST")
                    .antMatchers(GET, "/api/odk/submissions/**").hasAuthority("ODK_SUBMISSION_LIST")
                    .antMatchers(GET, "/api/odk/view/downloadSubmission").hasAuthority("ODK_SUBMISSION_DOWNLOAD")
                    .antMatchers(GET, "/api/odk/submission/**").hasAuthority("ODK_SUBMISSION_DOWNLOAD")
                    .anyRequest().authenticated()
                    .and()
                    .httpBasic();
        }
    }

    @Configuration
    public static class FormWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

        @Autowired
        AuthenticationSuccessHandler authSuccessHandler;

        protected void configure(HttpSecurity http) throws Exception {
            http
                    .headers().disable()
                    .csrf().disable()
                    .authorizeRequests()
                    .antMatchers("/css/**", "/images/**", "/webjars/**", "favicon.ico").permitAll()
                    .anyRequest().authenticated()
                    .and()
                    .formLogin()
                    .loginPage("/login")
                    .permitAll()
                    .successHandler(authSuccessHandler)
                    .and()
                    .logout()
                    .permitAll();
        }
    }
}
