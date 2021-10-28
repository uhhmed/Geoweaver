package com.gw.ssh;

import java.util.Arrays;

import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

// import com.gw.tools.SessionManager;


@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

	protected final Logger log = Logger.getLogger(getClass());

	// @Autowired
	// private SessionManager sessionManager;

	@Bean
	public SSHAuthenticationProvider sshAuthentication() {
		SSHAuthenticationProvider sshAuthentication = new SSHAuthenticationProvider();
		return sshAuthentication;
	}
	

	/*
	 * TODO - use 'registerAuthentication' in 3.2.0.RC1 and 'configure' in 3.2.0.RELEASE
	 * but note that 'configure' does not appear to work in tomcat7
	 */
	@Override
	protected void configure(AuthenticationManagerBuilder auth)
			throws Exception {
		log.info("registering SSH authentication provider");
		auth.authenticationProvider(sshAuthentication());
		// auth.inMemoryAuthentication().withUser("user").password("password").roles("USER");
		// auth.apply(new
		// SSHUserDetailsManagerConfigurer<AuthenticationManagerBuilder>());
	}
	
	/**
	 * WebSecurity is used to ask Spring Security to bypass the following resources
	 * WebSecurity is based on HttpSecurity
	 */
	@Override
	public void configure(WebSecurity builder) throws Exception {
		// builder.ignoring().antMatchers("/ssh/**").antMatchers("/static/**");
		// builder.ignoring().antMatchers("/Geoweaver/**");
	}

    @Override
    protected void configure(HttpSecurity http) throws Exception {
    	
    	http.authorizeRequests()
			.antMatchers("/Geoweaver/**")
			.permitAll()
			.and()
			.formLogin()
			.loginProcessingUrl("/Geoweaver/users/login")
			.and()
			.logout()
			.and();
    	
//    	http.authorizeRequests().anyRequest().authenticated();
    	
//    	http.authorizeRequests().antMatchers("/**").permitAll().anyRequest().authenticated();
    	
    	http.cors().and().csrf().disable();
    	
    	http.headers().frameOptions().disable();

		http.headers().disable(); //this must be turned off to make the JupyterHub work
    	
        // super.configure(http);
//        http
//            .authorizeRequests()
//                .antMatchers("/Geoweaver/web/ssh/**").permitAll()
//                .anyRequest().authenticated()
//                .and()
//            .formLogin()
//                .loginPage("/Geoweaver/web/ssh-login")
//                .permitAll()
//                .and()
//            .logout()
//                .logoutUrl("/Geoweaver/web/ssh-logout")
//                .permitAll();
    }
    
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
//        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("*"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

	@Bean 
	public PasswordEncoder passwordEncoder() { 
		return new BCryptPasswordEncoder(); 
	}
    
}
