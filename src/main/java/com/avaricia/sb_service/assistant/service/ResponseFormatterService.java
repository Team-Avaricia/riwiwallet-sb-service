package com.avaricia.sb_service.assistant.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Service responsible for formatting responses and utility functions.
 * Handles date formatting, emoji mapping, progress bars, and period translation.
 */
@Service
public class ResponseFormatterService {

    /**
     * Translates period values from English to Spanish for user display.
     */
    public String translatePeriod(String period) {
        if (period == null) return "Mensual";
        
        return switch (period.toLowerCase()) {
            case "monthly" -> "Mensual";
            case "weekly" -> "Semanal";
            case "biweekly" -> "Quincenal";
            case "yearly" -> "Anual";
            default -> period;
        };
    }

    /**
     * Formats a date string to a more readable format (DD/MM/YYYY).
     */
    public String formatDate(String isoDate) {
        try {
            LocalDate date = LocalDate.parse(isoDate);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return date.format(formatter);
        } catch (Exception e) {
            return isoDate;
        }
    }

    /**
     * Formats a date string from API (ISO format) to display format.
     */
    public String formatDateFromApi(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return "";
        try {
            // Handle ISO format: 2025-12-02T00:00:00Z or 2025-12-02
            String datePart = isoDate.contains("T") ? isoDate.substring(0, 10) : isoDate;
            String[] parts = datePart.split("-");
            if (parts.length >= 3) {
                return parts[2] + "/" + parts[1] + "/" + parts[0]; // DD/MM/YYYY
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return isoDate;
    }

    /**
     * Gets emoji for a category to make responses more visual.
     */
    public String getCategoryEmoji(String category) {
        if (category == null) return "📦";
        return switch (category.toLowerCase()) {
            case "comida" -> "🍔";
            case "transporte" -> "🚗";
            case "entretenimiento" -> "🎬";
            case "salud" -> "💊";
            case "educación" -> "📚";
            case "hogar" -> "🏠";
            case "ropa" -> "👕";
            case "tecnología" -> "📱";
            case "servicios" -> "💡";
            case "arriendo", "vivienda" -> "🏠";
            case "salario" -> "💼";
            case "freelance" -> "💻";
            case "inversiones" -> "📈";
            case "regalos" -> "🎁";
            default -> "📦";
        };
    }

    /**
     * Gets the emoji for an operation based on transaction type.
     */
    public String getOperationEmoji(String type) {
        if (type == null) return "📋";
        return switch (type) {
            case "Expense" -> "💸";
            case "Income" -> "💰";
            default -> "📋";
        };
    }

    /**
     * Gets the text for an operation type in Spanish.
     */
    public String getOperationTypeText(String type) {
        if (type == null) return "Operación";
        return switch (type) {
            case "Expense" -> "Gasto";
            case "Income" -> "Ingreso";
            default -> "Operación";
        };
    }

    /**
     * Generates a simple progress bar for percentages.
     */
    public String generateProgressBar(double percentage) {
        int filled = (int) Math.min(percentage / 10, 10);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < filled; i++) bar.append("█");
        for (int i = filled; i < 10; i++) bar.append("░");
        return bar.toString();
    }

    /**
     * Formats amount with thousand separators.
     */
    public String formatAmount(Double amount) {
        if (amount == null) return "$0";
        return String.format("$%,.0f", amount);
    }

    /**
     * Gets mock mode indicator text.
     */
    public String getMockIndicator(boolean useMock) {
        return useMock ? "\n\n🧪 _[Modo prueba]_" : "";
    }
}
