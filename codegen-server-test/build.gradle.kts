/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

description = "Generates Rust code from Smithy models and runs the protocol tests"
extra["displayName"] = "Smithy :: Rust :: Codegen :: Server :: Test"
extra["moduleName"] = "software.amazon.smithy.rust.kotlin.codegen.server.test"

tasks["jar"].enabled = false

plugins {
    java
    id("software.amazon.smithy.gradle.smithy-base")
    id("software.amazon.smithy.gradle.smithy-jar")
}

val smithyVersion: String by project
val properties = PropertyRetriever(rootProject, project)

val pluginName = "rust-server-codegen"
val workingDirUnderBuildDir = "smithyprojections/codegen-server-test/"

dependencies {
    implementation(project(":codegen-server"))
    implementation("software.amazon.smithy:smithy-aws-protocol-tests:$smithyVersion")
    implementation("software.amazon.smithy:smithy-protocol-test-traits:$smithyVersion")
    implementation("software.amazon.smithy:smithy-aws-traits:$smithyVersion")
    implementation("software.amazon.smithy:smithy-validation-model:$smithyVersion")
}

smithy {
    format.set(false)
}

val allCodegenTests = "../codegen-core/common-test-models".let { commonModels ->
    listOf(
        CodegenTest("crate#Config", "naming_test_ops", imports = listOf("$commonModels/naming-obstacle-course-ops.smithy")),
        CodegenTest("casing#ACRONYMInside_Service", "naming_test_casing", imports = listOf("$commonModels/naming-obstacle-course-casing.smithy")),
        CodegenTest(
            "naming_obs_structs#NamingObstacleCourseStructs",
            "naming_test_structs",
            imports = listOf("$commonModels/naming-obstacle-course-structs.smithy"),
        ),
        CodegenTest("com.amazonaws.simple#SimpleService", "simple", imports = listOf("$commonModels/simple.smithy")),
        CodegenTest(
            "com.amazonaws.constraints#ConstraintsService",
            "constraints_without_public_constrained_types",
            imports = listOf("$commonModels/constraints.smithy"),
            extraConfig = """, "codegen": { "publicConstrainedTypes": false } """,
        ),
        CodegenTest(
            "com.amazonaws.constraints#UniqueItemsService",
            "unique_items",
            imports = listOf("$commonModels/unique-items.smithy"),
        ),
        CodegenTest(
            "com.amazonaws.constraints#ConstraintsService",
            "constraints",
            imports = listOf("$commonModels/constraints.smithy"),
        ),
        CodegenTest("aws.protocoltests.restjson#RestJson", "rest_json"),
        CodegenTest(
            "aws.protocoltests.restjson#RestJsonExtras",
            "rest_json_extras",
            imports = listOf(
                "$commonModels/rest-json-extras.smithy",
                // TODO(https://github.com/smithy-lang/smithy/pull/2310): Can be deleted when consumed in next Smithy version.
                "$commonModels/rest-json-extras-2310.smithy",
                // TODO(https://github.com/smithy-lang/smithy/pull/2314): Can be deleted when consumed in next Smithy version.
                "$commonModels/rest-json-extras-2314.smithy",
                // TODO(https://github.com/smithy-lang/smithy/pull/2315): Can be deleted when consumed in next Smithy version.
                // TODO(https://github.com/smithy-lang/smithy/pull/2331): Can be deleted when consumed in next Smithy version.
                "$commonModels/rest-json-extras-2315.smithy",
            ),
        ),
        CodegenTest(
            "aws.protocoltests.restjson.validation#RestJsonValidation",
            "rest_json_validation",
            // `@range` trait is used on floating point shapes, which we deliberately don't want to support.
            // See https://github.com/smithy-lang/smithy-rs/issues/1401.
            extraConfig = """, "codegen": { "ignoreUnsupportedConstraints": true } """,
        ),
        CodegenTest("aws.protocoltests.json10#JsonRpc10", "json_rpc10"),
        CodegenTest("aws.protocoltests.json#JsonProtocol", "json_rpc11"),
        CodegenTest(
            "aws.protocoltests.misc#MiscService",
            "misc",
            imports = listOf("$commonModels/misc.smithy"),
        ),
        CodegenTest("com.amazonaws.ebs#Ebs", "ebs", imports = listOf("$commonModels/ebs.json")),
        CodegenTest("com.amazonaws.s3#AmazonS3", "s3"),
        CodegenTest(
            "com.aws.example#PokemonService",
            "pokemon-service-server-sdk",
            imports = listOf("$commonModels/pokemon.smithy", "$commonModels/pokemon-common.smithy"),
        ),
        CodegenTest(
            "com.aws.example#PokemonService",
            "pokemon-service-awsjson-server-sdk",
            imports = listOf("$commonModels/pokemon-awsjson.smithy", "$commonModels/pokemon-common.smithy"),
        ),
    )
}

project.registerGenerateSmithyBuildTask(rootProject, pluginName, allCodegenTests)
project.registerGenerateCargoWorkspaceTask(rootProject, pluginName, allCodegenTests, workingDirUnderBuildDir)
project.registerGenerateCargoConfigTomlTask(layout.buildDirectory.dir(workingDirUnderBuildDir).get().asFile)

tasks["smithyBuild"].dependsOn("generateSmithyBuild")
tasks["assemble"].finalizedBy("generateCargoWorkspace", "generateCargoConfigToml")

project.registerModifyMtimeTask()
project.registerCargoCommandsTasks(layout.buildDirectory.dir(workingDirUnderBuildDir).get().asFile)

tasks["test"].finalizedBy(cargoCommands(properties).map { it.toString })

tasks["clean"].doFirst { delete("smithy-build.json") }
