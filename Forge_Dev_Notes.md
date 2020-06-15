Forge Dev Notes
===============

Dev Environment Setup
---------------------

Environmental prerequisites for the build to work correctly:

```cmd
set JAVA_HOME=c:\Program Files\Java\jdk1.8.0_241
set PATH=%JAVA_HOME%\bin;%PATH%
```
	
Initial setup:

```cmd
gradlew setup
```
	
Docs for IDEA setup:
https://mcforge.readthedocs.io/en/1.12.x/forgedev/

Traps:

* Following these as-is will set up `projects/build.gradle` as JDK 11.
  Set back to JDK 8 in Gradle settings.
* Turning off "Optimize imports on the fly" is highly recommended, to
  reduce patch sizes. This is a per-project setting which won't affect
  your daily development in IDEA.


Making Changes
--------------

If the change is in the code for Minecraft itself:	

1. Make edits in the `projects/forge` sub-project.
2. Test it by running the 'forge client' run config	
3. When you're happy, run:

    ```cmd
    gradlew genPatches
    ```

4. Commit the patch files which are created.


Branches
--------

Branch with none of our changes:
[1.12.x](https://github.com/ephemeral-laboratories/MinecraftForge/tree/1.12.x)

Branch with the changes on it:
[1.12.x-i18n-fixes](https://github.com/ephemeral-laboratories/MinecraftForge/tree/1.12.x-i18n-fixes)

So far this branch contains nothing dozenal-specific
