package com.moneylens.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI moneyLensOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MoneyLens API")
                        .description("""
                                AI-Powered Personal Finance Intelligence Platform

                                Upload bank transactions → AI categorizes, predicts, and generates daily digests.

                                ### Features
                                - 📤 CSV Transaction Upload with AI Categorization
                                - 📊 Spending Prediction using Linear Regression + AI
                                - 🚨 Anomaly Detection for unusual spending patterns
                                - 📱 Daily AI-Generated Financial Digests
                                - 🎯 Savings Goals with AI Feasibility Analysis
                                - 📈 Business Intelligence Dashboard

                                ### Free AI API
                                Uses Google Gemini 2.0 Flash (free tier: 60 req/min)
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("MoneyLens")
                                .url("https://github.com/yourusername/moneylens"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
