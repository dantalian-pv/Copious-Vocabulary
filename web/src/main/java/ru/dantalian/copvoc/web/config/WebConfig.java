package ru.dantalian.copvoc.web.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.extras.springsecurity5.dialect.SpringSecurityDialect;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;

import ru.dantalian.copvoc.persist.api.PersistPrincipalManager;
import ru.dantalian.copvoc.web.security.DbUserDetailsService;
import ru.dantalian.copvoc.web.tpl.MixedDelegateTemplateResolver;

@Configuration
@EnableWebSecurity
public class WebConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	private PersistPrincipalManager principalManager;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private ThymeleafProperties properties;

  @Override
  protected void configure(final HttpSecurity aHttp) throws Exception {
    aHttp.exceptionHandling()
    				.authenticationEntryPoint(new AuthEntryPoint("/login"))
    		.and()
    		.authorizeRequests()
    				.antMatchers("/semantic/**", "/css/**", "/js/**").permitAll()
            .antMatchers("/").permitAll()
            .anyRequest().authenticated()
        .and()
        .formLogin()
            .loginPage("/login")
            .permitAll()
        .and()
        .logout()
            .permitAll();
  }

  @Bean
  @Override
  public UserDetailsService userDetailsService() {
    return new DbUserDetailsService(principalManager);
  }

  @Bean
  public SpringTemplateEngine templateEngine() {
      final SpringTemplateEngine templateEngine = new SpringTemplateEngine();
      templateEngine.setTemplateResolver(thymeleafTemplateResolver());
      templateEngine.addDialect(securityTemplateDialect());
      return templateEngine;
  }

  @Bean
  public SpringResourceTemplateResolver thymeleafTemplateResolver() {
      final MixedDelegateTemplateResolver resolver
        = new MixedDelegateTemplateResolver();
      resolver.setApplicationContext(this.applicationContext);
			resolver.setPrefix(this.properties.getPrefix());
			resolver.setSuffix(this.properties.getSuffix());
			resolver.setTemplateMode(this.properties.getMode());
			if (this.properties.getEncoding() != null) {
				resolver.setCharacterEncoding(this.properties.getEncoding().name());
			}
			resolver.setCacheable(this.properties.isCache());
			final Integer order = this.properties.getTemplateResolverOrder();
			if (order != null) {
				resolver.setOrder(order);
			}
			resolver.setCheckExistence(this.properties.isCheckTemplate());
      return resolver;
  }

  @Bean
  public SpringSecurityDialect securityTemplateDialect() {
  	return new SpringSecurityDialect();
  }

  @Bean
	public RestTemplate restTemplate(final RestTemplateBuilder builder) {
		return builder
				.setConnectTimeout(Duration.ofSeconds(10))
				.setReadTimeout(Duration.ofSeconds(10))
				.build();
	}

}
