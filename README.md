Nette for PhpStorm and IntelliJ Idea
=========================================

[![JetBrains Marketplace](https://img.shields.io/jetbrains/plugin/v/28342-nette-helpers?label=marketplace)](https://plugins.jetbrains.com/plugin/28342-nette-helpers)
[![Build](https://img.shields.io/github/actions/workflow/status/noctud/intellij-nette/build.yaml?branch=main)](https://github.com/noctud/intellij-nette/actions)
![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)
[![Discord](https://img.shields.io/badge/discord-join-5865F2?logo=discord&logoColor=white)](https://discord.noctud.dev)

<!-- Plugin description -->
A lightweight PhpStorm plugin that provides smart IDE support for the [Nette Framework](https://nette.org/). It adds autocompletion, type inference, and implicit usage detection for presenters and components.

If you have any problems with the plugin, [create an issue](https://github.com/noctud/intellij-nette/issues/new/choose) or join the [Noctud Discord](https://discord.noctud.dev).
<!-- Plugin description end -->

Installation
------------
Settings → Plugins → Marketplace → Search for "Nette Helpers" → Install → Apply


Installation from .jar file
------------
Download the `instrumented.jar` file from the [latest release](https://github.com/noctud/intellij-nette/releases) or the latest successful [GitHub Actions build](https://github.com/noctud/intellij-nette/actions).


Supported Features
------------------

* Autocompletion for controls in presenters and components
* Type inference for controls in presenters and components
* Marking presenter methods (actions, signals, etc.) as implicitly used

Building
------------

```sh
./gradlew buildPlugin
```

Testing in sandbox IDE
------------

```sh
./gradlew runIde
```
