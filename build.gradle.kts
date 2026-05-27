plugins {
    java
    jacoco
    id("org.springframework.boot") version "4.0.0-M1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.movieobserver"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}


dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.liquibase:liquibase-core")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect")
    implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")
    runtimeOnly("org.postgresql:postgresql")
    
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
    
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.platform:junit-platform-console:1.13.4")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("-Dfile.encoding=UTF-8")
    systemProperty("file.encoding", "UTF-8")
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    executionData(files(layout.buildDirectory.file("jacoco/manual-test.exec")))
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.register<Copy>("copyTestDependencies") {
    from(configurations.testRuntimeClasspath)
    into(layout.buildDirectory.dir("dependencies"))
}

tasks.register<Copy>("copyJacocoAgent") {
    from(configurations.getByName("jacocoAgent"))
    into(layout.buildDirectory.dir("jacoco-agent"))
    rename { "jacocoagent.jar" }
}
