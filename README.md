Dozenal Minecraft
=================

A crazy experiment of integrating dozenal support into Minecraft.

What is Dozenal?
----------------

Dozenal is one of the popularised names for a positional number system using twelve
as the base. Proponents of the dozenal system claim that it is superior to decimal
because many common fractions are more neatly represented in dozenal.

For example:

* 1/3 = 0.4
* 1/4 = 0.3
* 1/5 = 0.24972497...
* 1/6 = 0.2

For this project I intend to use the Unicode ↊ for ten/dec and ↋ for eleven/el,
though with slightly modified glyphs to fit the artwork I use elsewhere.


Getting it Running
------------------

I'll try to reduce the number of steps required over time, but here's the instructions
to get up and running at present:

1. Install latest [Java 8](https://www.java.com/en/download/)
2. Download the latest release of the
   [dozenal locale provider](https://github.com/ephemeral-laboratories/dozenal/releases),
   and place this inside the Java install directory, under `jre/lib/ext`.
   This won't break your existing Java applications as we'll set a very specific locale
   to actually activate dozenal formatting
3. Install [MultiMC](https://multimc.org/)
4. Download the latest modpack zip from
   [here](https://github.com/ephemeral-laboratories/dozenal-minecraft/releases)
5. In MultiMC, Add Instance, import from zip. Edit instance, check that the Java installation
   is the one you set earlier. The arguments to pass to the JVM should already be present
   in the text area below that.

Design Outline
--------------

Generally you would expect something like converting Minecraft to base 12 to be an
impossible task. There are too many places where numbers can appear. Too much code
which needs to be changed for such a specific purpose with no obvious benefit to most
players.

Sure, that may be true.

However, a feature of Java many people overlook is the "locale service provider
interface", `LocaleServiceProvider`.

`LocaleServiceProvider` allows one to register new locales. The intended use is when
you have a legitimately new locale which is not yet supported by Java. In fact,
it allows customising many locale-specific things:

* Locale names (`LocaleNameProvider`)
* Break iterators (`BreakIteratorProvider`)
* Calendars (`CalendarDataProvider`, `CalendarNameProvider`)
* Collators (`CollatorProvider`)
* Currencies (`CurrencyNameProvider`)
* Time zones (`TimeZoneNameProvider`)
* Date formats (`DateFormatProvider`, `DateFormatSymbolsProvider`)

Most importantly for us, it also allows customising:

* Number formats (`NumberFormatProvider`, `DecimalFormatSymbolsProvider`)

So the basic outline of how this can work is:

* Write a new locale service provider which provides base 12 number formatting,
  and potentially also parsing.
* Set this new locale when starting the game.
* Use Minecraft Forge to help apply patches to the game.
* Patch the game (and Forge) such that any time the game displays a number,
  `NumberFormat` is used. (But of course, not `DecimalFormat`.)
  **Note:** This particular change is worth doing in most software anyway,
  as some locales already like to format their numbers differently, and not
  using proper number formatting can result in numbers looking wrong for
  those users.
* Provide a custom resource pack to provide glyphs for the additional digits
  required.
* Optionally, do this for some mods as well, so that we can have a modded
  play-through in dozenal.

The Code
--------

This repository contains a resource pack which adds the missing glyphs to
Minecraft's font, both to the standard font and the legacy Unicode page.

It is also used as a central point to keep track of the project as a whole.

The bulk of the actual changes are in the relevant repository for each
respective change.

I will list these repositories below.

### Minecraft Forge

https://github.com/ephemeral-laboratories/MinecraftForge

Forge is one of the platforms for modding Minecraft.

Relevant branch: [1.15.x-i18n-fixes](https://github.com/ephemeral-laboratories/MinecraftForge/tree/1.15.x-i18n-fixes)

Changes to Forge are so far not dozenal-specific.


Building this Resource Pack
---------------------------

Prerequisites for running the build:

* `JAVA_HOME` is set and points to a JDK 8 installation.
  The build will not work on newer versions!
* Nothing else? Not to my knowledge, anyway. Gradle should set up the rest.

To build:

```
gradlew build
```

This will produce a distribution in `build/dist` which is a standard Minecraft resource pack.
