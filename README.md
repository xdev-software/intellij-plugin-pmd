[![Latest version](https://img.shields.io/jetbrains/plugin/v/pluginId?logo=jetbrains)](https://plugins.jetbrains.com/plugin/pluginId)
[![Build](https://img.shields.io/github/actions/workflow/status/xdev-software/intellij-plugin-pmd/check-build.yml?branch=develop)](https://github.com/xdev-software/intellij-plugin-pmd/actions/workflows/check-build.yml?query=branch%3Adevelop)
[![Feel free to leave a rating](https://img.shields.io/jetbrains/plugin/r/rating/pluginId?style=social&logo=jetbrains&label=Feel%20free%20to%20leave%20a%20rating)](https://plugins.jetbrains.com/plugin/pluginId/reviews)

# <img alt="Plugin icon" src="./src/main/resources/META-INF/pluginIcon.svg" width="30"> PMD X

A plugin for IntelliJ that provides code analysis and highlighting with <a href="https://pmd.github.io">PMD</a> - an extensible cross-language static code analyzer.

## Features
* Optimized for real-time analysis and zero configuration project setups that just require you to checkout the code
* Real-time analysis and highlighting of currently edited file
* Run bulk analysis of files by right clicking the project menu <br/> <img src="./assets/run-bulk-analysis.avif"> <br/> <img src="./assets/toolwindow-report.avif">
* Currently Java and Kotlin are supported, Language versions are automatically detected

## Usage
1. Install the plugin and open a project
2. Configure PMD for the project (`Settings > Tools > PMD`) <br/><img height=500 src="./assets/project-configuration.avif">
    * If there is already a project configuration file (`.idea/pmd-x.xml`) present this will be done automatically
3. Open a supported file
4. You should see the problems being automatically highlighted in the file and find more details in the PMD tool window <br/> <img src="./assets/annotator.avif"> <br/> <img src="./assets/toolwindow-current-file.avif">

## Installation
[Installation guide for the latest release](https://github.com/xdev-software/intellij-plugin-pmd/releases/latest#Installation)

> [!TIP]  
> [Development versions](https://plugins.jetbrains.com/plugin/pluginId/versions/snapshot) can be installed by [adding the ``snapshot`` release channel as a plugin repository](https://www.jetbrains.com/help/idea/managing-plugins.html#repos):<br/>
> ``https://plugins.jetbrains.com/plugins/snapshot/list``

## Contributing
See the [contributing guide](./CONTRIBUTING.md) for detailed instructions on how to get started with our project.

## Acknowledgment
This plugin was inspired by existing plugin like [CheckStyle-IDEA](https://plugins.jetbrains.com/plugin/1065-checkstyle-idea), [Sonarlint](https://plugins.jetbrains.com/plugin/7973-sonarqube-for-ide) and "[PMD](https://plugins.jetbrains.com/plugin/1137-pmd)".
Please have a look at [`THIRD-PARTY-CREDITS`](./THIRD-PARTY-CREDITS.md) for more details.

<sub>Disclaimer: This is not an official PMD project and not associated</sub>
