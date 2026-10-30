package com.student.config;

import java.io.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	// Folder (relative to where the app runs) where uploaded student photos are stored
	@Value("${app.upload.dir:uploads/photos}")
	private String uploadDir;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {

		// Security token check for every POST (runs first)
		registry.addInterceptor(new CsrfInterceptor())
				.addPathPatterns("/**")
				.excludePathPatterns(
						"/css/**",
						"/js/**",
						"/images/**",
						"/uploads/**",
						"/favicon.ico"
				);

		// Login check. Uploaded photos are no longer public - only logged-in admins can open them.
		registry.addInterceptor(new LoginInterceptor())
				.addPathPatterns("/**")
				.excludePathPatterns(
						"/",
						"/welcome",
						"/login",
						"/register",
						"/register/save",
						"/css/**",
						"/js/**",
						"/images/**",
						"/favicon.ico"
				);
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		String absolutePath = new File(uploadDir).getAbsolutePath();
		registry.addResourceHandler("/uploads/**")
				.addResourceLocations("file:" + absolutePath + File.separator);
	}
}
