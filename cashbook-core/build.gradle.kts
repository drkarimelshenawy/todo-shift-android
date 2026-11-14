plugins { `java-library` }

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val integrityTasks = listOf(
    "LedgerTest", "QueryTest", "ExtrasTest", "ReorderTest",
    "CustomizationTest", "CompactAccountsTest", "DebtTest", "Release12Test",
    "SalaryIncomeIdempotencyTest"
).map { suite ->
    tasks.register<JavaExec>("run$suite") {
        dependsOn(tasks.testClasses)
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("com.todoshift.cashbook.core.$suite")
    }
}
tasks.check { dependsOn(integrityTasks) }
