# Spindle: Core

The main implementation of Spindle. It consists of implementations for ModLauncher's
`ILaunchPluginService` and `ITransformationService` service interfaces.

Spindle handles the launch process somewhat like Fabric's Knot launcher.
The main differences are:
- initialising Mixin: delegated to Mixin's own services
- modifying the classpath: hooked into ModLauncher's mod scan API instead of modifying a class loader directly
