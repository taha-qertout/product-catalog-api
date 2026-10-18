# Learning notes

These notes accompany the [project overview](../README.md). Run all commands from the repository root. The [verification record](verification.md) contains the existing test evidence.

## Your learning task

The MVP is complete so you have a working reference. Before applying with it, own a small piece of it:

1. Read the controller, then hide the implementation of `ProductService.findById` and reimplement it yourself. Use `repository.findById`, handle `Optional`, throw `ProductNotFoundException` for a missing id, and map the entity to `ProductResponse`.
2. Recreate its two service tests: existing product and missing product. Run `.\mvnw.cmd test`.
3. Explain aloud why the service does not return `ResponseEntity`, why the repository returns `Optional`, and where the 404 is decided.

Then trace POST from JSON to validation to service to INSERT to the 201 response. Be ready to explain `BigDecimal`, dependency injection, transactions, dirty checking, and why an integration test catches failures that Mockito cannot. Describe this honestly as an assisted project that you understand and extended yourself.
