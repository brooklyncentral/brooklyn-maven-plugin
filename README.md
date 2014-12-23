Brooklyn Maven Plugin
=====================

A Maven plugin to help test your [Apache
Brooklyn](https://brooklyn.incubator.apache.org/) blueprints.

### Download

The plugin is not currently published to Maven repositories.  For now you must
clone and install this project yourself.

<!-- Enable when plugin published.
Include this dependency in your pom:
```xml
<dependency>
    <groupId>io.brooklyn.maven</groupId>
    <artifactId>brooklyn-maven-plugin</artifactId>
    <version>(insert latest version)</version>
</dependency>
```
-->

### Goals

<dl>
<dt><strong>deploy</strong></dt>
<dd>
Instruct an existing Brooklyn server to deploy the given blueprint.
</dd>

<dt><strong>sensor</strong></dt>
<dd>
Instruct a Brooklyn server to stop the application with the given ID.
</dd>

<dt><strong>stop</strong></dt>
<dd>
Fetch the value of a sensor on entities at a given server whose types match
a regular expression.
</dd>

<dt><strong>help</strong></dt>
<dd>
Display help information on brooklyn-maven-plugin.
</dd>
</dl>
