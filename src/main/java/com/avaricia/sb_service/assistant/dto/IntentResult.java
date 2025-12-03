package com.avaricia.sb_service.assistant.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IntentResult {
    
    private String intent;
    private Double amount;
    private String category;
    private String description;
    private String type; // "Expense" or "Income"
    private String period; // "Monthly", "Weekly", etc.
    private String response; // Respuesta para el usuario cuando es una pregunta general
    
    // New fields for advanced queries
    private String frequency; // "Daily", "Weekly", "Monthly", "Yearly" for recurring transactions
    private Integer dayOfMonth; // For monthly recurring (1-31)
    private Integer dayOfWeek; // For weekly recurring (0=Sun, 1=Mon...)
    private String startDate; // For date range queries (ISO format)
    private String endDate; // For date range queries (ISO format)
    private String searchQuery; // For searching transactions by description
    
    // Constructors
    public IntentResult() {}
    
    // Getters and Setters
    public String getIntent() {
        return intent;
    }
    
    public void setIntent(String intent) {
        this.intent = intent;
    }
    
    public Double getAmount() {
        return amount;
    }
    
    public void setAmount(Double amount) {
        this.amount = amount;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getPeriod() {
        return period;
    }
    
    public void setPeriod(String period) {
        this.period = period;
    }
    
    public String getResponse() {
        return response;
    }
    
    public void setResponse(String response) {
        this.response = response;
    }
    
    public String getFrequency() {
        return frequency;
    }
    
    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }
    
    public Integer getDayOfMonth() {
        return dayOfMonth;
    }
    
    public void setDayOfMonth(Integer dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }
    
    public Integer getDayOfWeek() {
        return dayOfWeek;
    }
    
    public void setDayOfWeek(Integer dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }
    
    public String getStartDate() {
        return startDate;
    }
    
    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }
    
    public String getEndDate() {
        return endDate;
    }
    
    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }
    
    public String getSearchQuery() {
        return searchQuery;
    }
    
    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }
    
    @Override
    public String toString() {
        return "IntentResult{" +
                "intent='" + intent + '\'' +
                ", amount=" + amount +
                ", category='" + category + '\'' +
                ", description='" + description + '\'' +
                ", type='" + type + '\'' +
                ", period='" + period + '\'' +
                ", response='" + response + '\'' +
                ", frequency='" + frequency + '\'' +
                ", dayOfMonth=" + dayOfMonth +
                ", startDate='" + startDate + '\'' +
                ", endDate='" + endDate + '\'' +
                ", searchQuery='" + searchQuery + '\'' +
                '}';
    }
}
