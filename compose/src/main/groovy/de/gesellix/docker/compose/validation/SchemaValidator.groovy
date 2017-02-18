package de.gesellix.docker.compose.validation

//import com.github.fge.jackson.JsonLoader
//import com.github.fge.jsonschema.core.report.ProcessingReport
//import com.github.fge.jsonschema.main.JsonSchemaFactory
//import groovy.json.JsonOutput
//import groovy.util.logging.Slf4j
//
//@Slf4j
//class SchemaValidator {
//
//    def validate(composeContent) {
//        def version = sanitize(composeContent.version)
//
//        def schemaUrl = getClass().getResource("/compose/config_schema_v${version}.json")
//        def schemaNode = JsonLoader.fromURL(schemaUrl)
//        final JsonSchemaFactory factory = JsonSchemaFactory.byDefault()
//        def schema = factory.getJsonSchema(schemaNode)
//
//        def json = JsonLoader.fromString(JsonOutput.toJson(composeContent))
//
//        ProcessingReport report = schema.validate(json)
//        log.info("schema validation report: ${report}")
//        return report.isSuccess()
//    }
//
//    def sanitize(versionString) {
//        if (versionString == "3") {
//            return "3.0"
//        } else {
//            return versionString
//        }
//    }
//}
