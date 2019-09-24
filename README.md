# Unreal Packages Dependencies

A small command-line tool to analyse and report on the dependencies of Unreal,
Unreal Tournament (UT99), UT2004 and other Unreal Engine 1 and 2 packages.

Given a path to a game content directory, and one or more packages (typically 
maps) to analyse, the tool will perform a deep inspection of the packages,
classes, sounds, textures, models and more required by the analysed files, and
verify that the required files and contents are present.

The primary use-case is to perform batch analysis of maps and their 
requirements, typically for server administrators, or players who just want to
make sure their maps are all clean and in working order.

*Requirements:*
- Java 11 (OpenJDK, Zulu, Oracle, etc)


## Building

The project is built with Gradle. The provided `gradlew` wrapper may be 
invoked as follows to produce an executable Jar file:

### On Linux

```
./gradlew execJar
```

To run, execute:

```
./build/libs/package-dependencies
# - or via java -
java -jar build/libs/package-dependencies-exec.jar
```

### On Windows

```
gradlew.bat execJar
```

To run, execute:

```
java -jar build\libs\unreal-archive-exec.jar
```


## Usage

![Inspecting Maps](https://i.imgur.com/SYoDN0g.gif)

> Note: Linux executable used here, follow above advice for executing on 
> Windows.

```
./package-dependency [options] <search-path> <package, ...>
```

- `search-path`:
  - path to an Unreal, UT, or other game root directory
- `package`:
  - multiple entries may be provided, space-separated
  - names or paths of the content to analyse. for example:
    `CTF-Face` or `./Maps/CTF-Face.unr`
- `options`:
  - `--show=[all,files,packages,missing_packages,missing_detail]`
    - level of detail to report.
    - `all`: show full detail of all imported objects
    - `files`: show summary represented as checked files only
    - `packages`: under each file, show the packages imported
    - `missing_packages`: only show files and packages with unresolved objects, 
      without additional detail
    - `missing_detail`: only show files with unresolved objects, but print the
      full tree of missing objects

An exit code of `1` will be returned if any of the analysed packages have any
missing dependencies. This makes it easy to automate or script routine 
inspections via `cron` or other tooling.
