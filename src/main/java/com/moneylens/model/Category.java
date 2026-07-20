package com.moneylens.model;

public enum Category {
    FOOD("Food & Dining", "🍜"),
    TRANSPORT("Transportation", "🚗"),
    SALARY("Salary & Income", "💰"),
    SUBSCRIPTION("Subscriptions", "📱"),
    RENT("Rent & Housing", "🏠"),
    INVESTMENT("Investment", "📈"),
    ENTERTAINMENT("Entertainment", "🎬"),
    UTILITIES("Utilities", "⚡"),
    HEALTHCARE("Healthcare", "🏥"),
    SHOPPING("Shopping", "🛒"),
    EDUCATION("Education", "📚"),
    OTHER("Other", "📌");

    private final String displayName;
    private final String emoji;

    Category(String displayName, String emoji) {
        this.displayName = displayName;
        this.emoji = emoji;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmoji() {
        return emoji;
    }
}
