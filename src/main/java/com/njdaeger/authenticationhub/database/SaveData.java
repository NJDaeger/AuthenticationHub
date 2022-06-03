package com.njdaeger.authenticationhub.database;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents a field that is saved into an application's database.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface SaveData {

    /**
     * The name of the referenced field
     * @return The name of the referenced field, or an empty string if the reference field has the same name as the current field.
     */
    String fieldName() default "";

    /**
     * How the corresponding SQL Table should interpret this saved data field.
     * @return The name of the column type. VarChar by default.
     */
    String columnType() default "varchar(64)";

    /**
     * The column number of this data field. (Only used for SQL database)
     * @return Column number of this data field.
     */
    int columnOrder();

}
