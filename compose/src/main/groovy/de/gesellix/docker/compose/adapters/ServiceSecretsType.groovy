package de.gesellix.docker.compose.adapters

import com.squareup.moshi.JsonQualifier

import java.lang.annotation.Retention

import static java.lang.annotation.RetentionPolicy.RUNTIME

@Retention(RUNTIME)
@JsonQualifier
@interface ServiceSecretsType {
}
