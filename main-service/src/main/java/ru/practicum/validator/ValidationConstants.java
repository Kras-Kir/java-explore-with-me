package ru.practicum.validator;

import java.time.format.DateTimeFormatter;

public class ValidationConstants {
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    public static final int MIN_EVENT_TITLE_LENGTH = 3;
    public static final int MAX_EVENT_TITLE_LENGTH = 120;
    public static final int MIN_EVENT_ANNOTATION_LENGTH = 20;
    public static final int MAX_EVENT_ANNOTATION_LENGTH = 2000;
    public static final int MIN_EVENT_DESCRIPTION_LENGTH = 20;
    public static final int MAX_EVENT_DESCRIPTION_LENGTH = 7000;

    public static final int MIN_CATEGORY_NAME_LENGTH = 1;
    public static final int MAX_CATEGORY_NAME_LENGTH = 50;

    public static final int MIN_USER_NAME_LENGTH = 2;
    public static final int MAX_USER_NAME_LENGTH = 250;
    public static final int MIN_USER_EMAIL_LENGTH = 6;
    public static final int MAX_USER_EMAIL_LENGTH = 254;

    public static final int MIN_COMPILATION_TITLE_LENGTH = 1;
    public static final int MAX_COMPILATION_TITLE_LENGTH = 50;

    public static final int HOURS_BEFORE_EVENT_FOR_USER = 2;
    public static final int HOURS_BEFORE_EVENT_FOR_ADMIN = 1;
}
