Dozenal Minecraft
=================

A crazy experiment of integrating dozenal support into Minecraft.

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

The repository you are currently looking at is intended as a central point to keep
track of the project as a whole. The actual changes are in the relevant repository
for each respective change.

I will list these repositories here.

### Minecraft Forge

https://github.com/ephemeral-laboratories/MinecraftForge

Forge is one of the platforms for modding Minecraft.

Relevant branch: [1.12.x-i18n-fixes](https://github.com/ephemeral-laboratories/MinecraftForge/tree/1.12.x-i18n-fixes)

Changes to Forge are so far not dozenal-specific.

### Mods

#### FTB Library

https://github.com/ephemeral-laboratories/FTB-Library

Relevant branch: [1.12-dozenal](https://github.com/ephemeral-laboratories/FTB-Library/tree/1.12-dozenal)
