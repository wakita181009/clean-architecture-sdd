---
description: Applies when writing or reviewing Kotlin test files
globs:
  - "**/*Test.kt"
  - "**/*Tests.kt"
---

# Test Conventions (Virtual Threads Project)

This project uses virtual threads — all methods are regular functions (not `suspend`).

## FORBIDDEN in test files

- `coEvery` / `coVerify` — use `every` / `verify` instead (methods are not `suspend`)
- `runTest { }` — not needed (no coroutines at interface boundaries)
- `runBlocking { }` — not needed (virtual threads handle concurrency)
- Kotest spec styles (`DescribeSpec`, `FunSpec`, `BehaviorSpec`) — use JUnit 5 `@Test` + `@Nested`
- `io.kotest.provided.ProjectConfig` — not needed with JUnit 5

## Required

- Use **JUnit 5** as the test runner (`@Test`, `@Nested`, `@BeforeEach`)
- Use `every { }` / `verify { }` for MockK stubbing and verification
- Use `clearMocks(...)` in `@BeforeEach` to reset mocks between tests
- Use **kotest-assertions** (`shouldBe`, `shouldBeRight`, `shouldBeLeft`) for assertions (assertions only, NOT the Kotest test runner)

## Exception assertion style

Use Kotest `shouldThrow<>` instead of try/catch for exception verification:

```kotlin
// FORBIDDEN — verbose and error-prone
try {
    controller.doSomething(...)
    throw AssertionError("Expected exception")
} catch (e: ResponseStatusException) {
    e.statusCode shouldBe HttpStatus.BAD_REQUEST
}

// CORRECT — concise and idiomatic
shouldThrow<ResponseStatusException> {
    controller.doSomething(...)
}.statusCode shouldBe HttpStatus.BAD_REQUEST
```

## Test class structure

```kotlin
@SpringBootTest // only for integration tests
@ActiveProfiles("test") // only for integration tests
class XxxTest {

    @Nested
    inner class SomeFeature {
        @Test
        fun `returns Right for valid input`() {
            // ...
        }
    }
}
```
